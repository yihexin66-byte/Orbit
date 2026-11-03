#!/usr/bin/env node
'use strict'

/**
 * Orbit AI smoke test.
 *
 * Reads secrets from the environment only; nothing is written to disk and the
 * password is never printed. By default it calls the free status endpoint.
 * --plan, --review and --chat make real Coze calls and consume daily quota.
 *
 * Usage:
 *   ORBIT_USERNAME=... ORBIT_PASSWORD=... node unicloud/tools/check-ai.mjs
 *   ORBIT_USERNAME=... ORBIT_PASSWORD=... node unicloud/tools/check-ai.mjs --plan
 *   ORBIT_USERNAME=... ORBIT_PASSWORD=... node unicloud/tools/check-ai.mjs --review
 *   ORBIT_USERNAME=... ORBIT_PASSWORD=... node unicloud/tools/check-ai.mjs --chat
 */

const DEFAULT_BASE = 'https://env-00jy6ydc58ll.dev-hz.cloudbasefunction.cn/orbit'
const REQUEST_TIMEOUT_MS = 45_000

const base = (process.env.ORBIT_API_BASE || DEFAULT_BASE).replace(/\/+$/, '')
const username = process.env.ORBIT_USERNAME || ''
const password = process.env.ORBIT_PASSWORD || ''
const wantsPlan = process.argv.includes('--plan')
const wantsReview = process.argv.includes('--review')
const wantsChat = process.argv.includes('--chat')

function fail(message) {
  console.error('✗ ' + message)
  process.exit(1)
}

function log(message) {
  console.log(message)
}

async function request(path, options = {}) {
  const response = await fetch(base + path, {
    ...options,
    signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    headers: {
      accept: 'application/json',
      ...(options.body ? { 'content-type': 'application/json' } : {}),
      ...(options.headers || {}),
    },
  })
  const text = await response.text()
  let json = null
  try {
    json = JSON.parse(text)
  } catch (error) {
    // Keep json null; the caller reports the raw text.
  }
  return { status: response.status, json, text }
}

function errorText(result) {
  if (result.json && (result.json.code || result.json.message || result.json.msg)) {
    return [result.json.code, result.json.message || result.json.msg]
      .filter(Boolean)
      .join(' · ')
  }
  return result.text.slice(0, 300) || 'empty response'
}

async function main() {
  if (!username || !password) {
    fail('请先设置 ORBIT_USERNAME 与 ORBIT_PASSWORD 环境变量（不会写入任何文件）。')
  }

  log('API: ' + base)

  const login = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
  if (login.status !== 200 || !login.json || !login.json.token) {
    fail('登录失败（HTTP ' + login.status + '）：' + errorText(login))
  }
  const token = login.json.token
  log('✓ 登录成功：' + (login.json.user && login.json.user.nickname ? login.json.user.nickname : username))

  const auth = { authorization: 'Bearer ' + token }

  const status = await request('/api/ai/status', { headers: auth })
  if (status.status !== 200 || !status.json) {
    fail('查询 AI 状态失败（HTTP ' + status.status + '）：' + errorText(status))
  }
  const data = status.json
  log('')
  log('configured: ' + Boolean(data.configured))
  log('planPromptVersion: ' + (data.planPromptVersion || '-'))
  log('reviewPromptVersion: ' + (data.reviewPromptVersion || '-'))
  log('chatPromptVersion: ' + (data.chatPromptVersion || '-'))
  log('timeoutSeconds: ' + data.timeoutSeconds)
  const limits = data.limits || {}
  const usage = data.usage || {}
  log('limits: plan ' + limits.plan + ' / review ' + limits.review + ' / chat ' + limits.chat)
  log('used today: plan ' + (usage.planUsed || 0) + ' / review ' + (usage.reviewUsed || 0) +
    ' / chat ' + (usage.chatUsed || 0))
  log('remaining today: plan ' + Math.max(0, (limits.plan || 0) - (usage.planUsed || 0)) +
    ' / review ' + Math.max(0, (limits.review || 0) - (usage.reviewUsed || 0)) +
    ' / chat ' + Math.max(0, (limits.chat || 0) - (usage.chatUsed || 0)))

  if (!data.configured) {
    log('')
    log('AI 尚未配置。请在 uniCloud 控制台给 orbit-api 添加 COZE_API_TOKEN 与 COZE_BOT_ID。')
    process.exitCode = 2
    return
  }

  if (wantsPlan) {
    log('')
    log('--- 生成学习计划（消耗 1 次 plan 配额）---')
    const today = Math.floor((Date.now() + 8 * 60 * 60 * 1000) / 86_400_000)
    const plan = await request('/api/ai/plan', {
      method: 'POST',
      headers: auth,
      body: JSON.stringify({
        goal: '8 周复习 Java 后端并完成一个可演示的 CRUD 项目',
        targetDateEpochDay: today + 56,
        dailyMinutes: 60,
        currentLevel: '会一点 Kotlin 和 SQL',
        weakPoints: 'Spring Boot、接口设计、联调',
      }),
    })
    if (plan.status !== 200 || !plan.json || !plan.json.plan) {
      fail('生成计划失败（HTTP ' + plan.status + '）：' + errorText(plan))
    }
    const value = plan.json.plan
    const stages = Array.isArray(value.stages) ? value.stages : []
    const tasks = stages.reduce(
      (total, stage) => total + (Array.isArray(stage.tasks) ? stage.tasks.length : 0),
      0,
    )
    log('✓ requestId: ' + plan.json.requestId + ' · ' + plan.json.promptVersion)
    log('  goalTitle: ' + value.goalTitle)
    log('  stages: ' + stages.length + ' · tasks: ' + tasks)
    for (const stage of stages) {
      log('  - ' + stage.title + '（截止日偏移 ' + (stage.targetDateEpochDay - today) + ' 天）')
      for (const task of (stage.tasks || []).slice(0, 3)) {
        log('      · ' + task.title + ' [' + task.priority + '] day+' + (task.dueDateEpochDay - today))
      }
    }
  }

  if (wantsReview) {
    log('')
    log('--- 生成周复盘（消耗 1 次 review 配额，使用合成数据）---')
    const today = Math.floor((Date.now() + 8 * 60 * 60 * 1000) / 86_400_000)
    const weekday = new Date(today * 86_400_000).getUTCDay()
    const weekStart = today - ((weekday + 6) % 7)
    const review = await request('/api/ai/review', {
      method: 'POST',
      headers: auth,
      body: JSON.stringify({
        weekStartEpochDay: weekStart,
        planned: 5,
        completed: 3,
        studyPlanned: 3,
        studyCompleted: 2,
        habitPossible: 7,
        habitCheckIns: 4,
        xp: 120,
        goals: [
          { title: 'Java 后端', progress: 0.4, stageCount: 4, completedStages: 1 },
        ],
      }),
    })
    if (review.status !== 200 || !review.json || !review.json.review) {
      fail('生成复盘失败（HTTP ' + review.status + '）：' + errorText(review))
    }
    const value = review.json.review
    log('✓ requestId: ' + review.json.requestId + ' · ' + review.json.promptVersion)
    log('  headline: ' + value.headline)
    log('  summary: ' + value.summary)
    log('  wins: ' + (value.wins || []).length + ' · risks: ' + (value.risks || []).length +
      ' · nextWeek: ' + (value.nextWeek || []).length)
  }

  if (wantsChat) {
    log('')
    log('--- 学习助手对话（消耗 1 次 chat 配额，使用聚合上下文）---')
    const chat = await request('/api/ai/chat', {
      method: 'POST',
      headers: auth,
      body: JSON.stringify({
        message: '我每天只有 30 分钟，怎么安排英语听力和单词复习？',
        history: [],
        context: {
          todayPlanned: 3,
          todayCompleted: 1,
          pendingCount: 5,
          completedCount: 12,
          habitTodayChecked: 1,
          habitTodayTotal: 2,
          totalXp: 420,
          thisWeekXp: 90,
          goals: [{ title: '英语六级', progress: 0.35 }],
        },
      }),
    })
    if (chat.status !== 200 || !chat.json || !chat.json.reply) {
      fail('对话失败（HTTP ' + chat.status + '）：' + errorText(chat))
    }
    log('✓ requestId: ' + chat.json.requestId + ' · ' + chat.json.promptVersion)
    log('  reply: ' + chat.json.reply)
  }

  if (!wantsPlan && !wantsReview && !wantsChat) {
    log('')
    log('提示：加 --plan、--review 或 --chat 会真实调用 Coze 并消耗每日配额。')
  }
}

main().catch((error) => {
  fail(error && error.message ? error.message : String(error))
})
