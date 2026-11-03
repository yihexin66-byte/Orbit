#!/usr/bin/env node
/**
 * Copies one Orbit account's snapshot from the Spring/MySQL backend to the
 * uniCloud backend through the public HTTP APIs. No npm dependencies.
 *
 *   node migrate.mjs --from https://old.example/ --to https://new.example/orbit/ \
 *        --from-user nova_1027 --to-user nova_1027 --dump snapshot.json
 *
 * Passwords are prompted interactively and never stored. The uniCloud account
 * is created with a new password; scrypt hashes cannot be copied from MySQL.
 */
'use strict'

import fs from 'node:fs'
import readline from 'node:readline/promises'
import { stdin, stdout } from 'node:process'

const SNAPSHOT_KEYS = [
  'goals',
  'stages',
  'tasks',
  'habits',
  'habitRecords',
  'growthEvents',
]

function parseArgs(argv) {
  const options = {
    from: '',
    to: '',
    fromUser: '',
    toUser: '',
    dump: '',
    dryRun: false,
    force: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--from') options.from = argv[++index] || ''
    else if (arg === '--to') options.to = argv[++index] || ''
    else if (arg === '--from-user') options.fromUser = argv[++index] || ''
    else if (arg === '--to-user') options.toUser = argv[++index] || ''
    else if (arg === '--dump') options.dump = argv[++index] || ''
    else if (arg === '--dry-run') options.dryRun = true
    else if (arg === '--force') options.force = true
    else if (arg === '--help' || arg === '-h') {
      printHelp()
      process.exit(0)
    } else {
      throw new Error('未知参数：' + arg)
    }
  }
  return options
}

function printHelp() {
  console.log(`用法：
  node migrate.mjs --from <旧后端 Base URL> --to <uniCloud URL化 Base URL> \
    [--from-user <用户名>] [--to-user <用户名>] [--dump <文件>] [--dry-run] [--force]

说明：
  --from       Spring/MySQL 后端地址，例如 https://old.example/
  --to         uniCloud URL化 地址，例如 https://<spaceId>.bspapp.com/orbit/
  --dump       先把旧账号快照写入本地 JSON 备份
  --dry-run    只导出，不写入新后端
  --force      允许覆盖新账号里已存在的非空快照
密码会在终端中交互输入，不会写入命令行历史。`)
}

function normalizeBase(base) {
  if (!base) throw new Error('Base URL 不能为空')
  const value = /^https?:\/\//i.test(base) ? base : 'https://' + base
  return value.endsWith('/') ? value : value + '/'
}

function urlFor(base, path) {
  return new URL(String(path).replace(/^\/+/, ''), normalizeBase(base)).toString()
}

async function request(base, path, { method = 'GET', body, token } = {}) {
  const headers = { accept: 'application/json' }
  if (body !== undefined) headers['content-type'] = 'application/json'
  if (token) headers.authorization = 'Bearer ' + token
  let response
  try {
    response = await fetch(urlFor(base, path), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(30000),
    })
  } catch (error) {
    throw new Error('无法连接 ' + base + '：' + error.message)
  }
  const text = await response.text()
  let payload = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch (error) {
      payload = text
    }
  }
  return { status: response.status, payload }
}

function apiMessage(result) {
  if (result.payload && typeof result.payload === 'object') {
    if (result.payload.message) return result.payload.message
    if (result.payload.code) return result.payload.code
  }
  return typeof result.payload === 'string' && result.payload
    ? result.payload
    : 'HTTP ' + result.status
}

function snapshotCounts(snapshot) {
  return SNAPSHOT_KEYS.map((key) => key + '=' + (Array.isArray(snapshot[key]) ? snapshot[key].length : 0)).join(' ')
}

function snapshotBody(snapshot, baseRevision) {
  const body = { baseRevision }
  for (const key of SNAPSHOT_KEYS) {
    body[key] = Array.isArray(snapshot[key]) ? snapshot[key] : []
  }
  return body
}

function hasRows(snapshot) {
  return SNAPSHOT_KEYS.some((key) => Array.isArray(snapshot[key]) && snapshot[key].length > 0)
}

async function login(base, username, password, allowRegister) {
  if (allowRegister) {
    const registered = await request(base, 'api/auth/register', {
      method: 'POST',
      body: { username, password },
    })
    if (registered.status === 201) return registered.payload.token
    if (registered.status !== 409) {
      throw new Error('注册新账号失败：' + apiMessage(registered))
    }
    console.log('新后端已存在该用户名，改用登录。')
  }

  const loggedIn = await request(base, 'api/auth/login', {
    method: 'POST',
    body: { username, password },
  })
  if (loggedIn.status !== 200 || !loggedIn.payload || !loggedIn.payload.token) {
    throw new Error('登录失败：' + apiMessage(loggedIn))
  }
  return loggedIn.payload.token
}

async function main() {
  const options = parseArgs(process.argv.slice(2))
  const rl = readline.createInterface({ input: stdin, output: stdout })
  const ask = async (question) => (await rl.question(question)).trim()

  try {
    options.from = normalizeBase(options.from || (await ask('旧后端 Base URL：')))
    options.fromUser = options.fromUser || (await ask('旧账号用户名：'))
    const fromPassword = await ask('旧账号密码：')
    if (!options.fromUser || !fromPassword) {
      throw new Error('旧账号用户名和密码不能为空')
    }

    console.log('登录旧后端…')
    const oldToken = await login(options.from, options.fromUser, fromPassword, false)
    const oldSnapshot = await request(options.from, 'api/sync', { token: oldToken })
    if (oldSnapshot.status !== 200) {
      throw new Error('读取旧快照失败：' + apiMessage(oldSnapshot))
    }
    const snapshot = oldSnapshot.payload
    console.log('旧快照 revision=' + snapshot.revision + ' ' + snapshotCounts(snapshot))

    if (options.dump) {
      fs.writeFileSync(options.dump, JSON.stringify(snapshot, null, 2), 'utf8')
      console.log('已备份到 ' + options.dump)
    }
    if (options.dryRun) {
      console.log('dry-run：未写入新后端。')
      return
    }

    options.to = normalizeBase(options.to || (await ask('uniCloud URL化 Base URL：')))
    const toUser = options.toUser || options.fromUser
    const toPassword = await ask('新账号密码（uniCloud 需重新设置）：')
    if (!toPassword || toPassword.length < 6) {
      throw new Error('新密码至少 6 位')
    }

    console.log('准备新后端账号 ' + toUser + '…')
    const newToken = await login(options.to, toUser, toPassword, true)
    const current = await request(options.to, 'api/sync', { token: newToken })
    if (current.status !== 200) {
      throw new Error('读取新快照失败：' + apiMessage(current))
    }
    if (
      hasRows(current.payload) &&
      Number(current.payload.revision) > 0 &&
      !options.force
    ) {
      throw new Error('新账号已有云端数据（revision=' + current.payload.revision + '）。' +
        '确认要覆盖请加 --force。')
    }

    console.log('写入新后端…')
    const pushed = await request(options.to, 'api/sync', {
      method: 'PUT',
      token: newToken,
      body: snapshotBody(snapshot, Number(current.payload.revision) || 0),
    })
    if (pushed.status !== 200) {
      throw new Error('写入失败：' + apiMessage(pushed))
    }
    console.log('迁移完成：revision=' + pushed.payload.revision + ' ' + snapshotCounts(pushed.payload))
  } finally {
    rl.close()
  }
}

main().catch((error) => {
  console.error('迁移失败：' + error.message)
  process.exitCode = 1
})
