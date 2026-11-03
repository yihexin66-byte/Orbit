package com.example.orbit.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.orbit.data.local.OrbitDatabase
import com.example.orbit.model.TaskCategory
import com.example.orbit.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: OrbitDatabase
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, OrbitDatabase::class.java)
            .build()
        repository = TaskRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createReadObserveFilterAndDelete() = runBlocking {
        val firstId = repository.createTask(
            title = "  完成作品集数据层  ",
            description = "   ",
            priority = TaskPriority.HIGH,
            category = TaskCategory.STUDY,
            dueDate = 20_000L,
        )
        val secondId = repository.createTask(
            title = "跑步 5 公里",
            description = "晚间轻松跑",
            priority = TaskPriority.LOW,
            category = TaskCategory.HEALTH,
            dueDate = 20_001L,
        )

        val first = repository.getTask(firstId)
        assertNotNull(first)
        assertEquals("完成作品集数据层", first!!.title)
        assertNull(first.description)
        assertTrue(first.createdAt > 0L)

        repository.setCompleted(secondId, true)

        assertEquals(2, repository.observeTasks().first().size)
        assertEquals(
            listOf(firstId),
            repository.observeTasksByCompletion(completed = false).first().map { it.id },
        )
        assertEquals(
            listOf(secondId),
            repository.observeTasksByCompletion(completed = true).first().map { it.id },
        )

        repository.deleteTask(firstId)
        assertNull(repository.getTask(firstId))
        assertEquals(listOf(secondId), repository.observeTasks().first().map { it.id })
    }

    @Test
    fun completingRecordsTimestampAndReopeningClearsIt() = runBlocking {
        val id = repository.createTask(
            title = "复习 Room",
            description = null,
            priority = TaskPriority.MEDIUM,
            category = TaskCategory.STUDY,
            dueDate = null,
        )

        repository.setCompleted(id, true)
        val completed = repository.getTask(id)!!
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)
        assertTrue(completed.completedAt!! > 0L)

        repository.setCompleted(id, false)
        val reopened = repository.getTask(id)!!
        assertFalse(reopened.isCompleted)
        assertNull(reopened.completedAt)
    }

    @Test
    fun updateKeepsCompletionStateAndCreatedAt() = runBlocking {
        val id = repository.createTask(
            title = "旧标题",
            description = "旧描述",
            priority = TaskPriority.LOW,
            category = TaskCategory.LIFE,
            dueDate = 20_000L,
        )
        repository.setCompleted(id, true)
        val before = repository.getTask(id)!!

        repository.updateTask(
            id = id,
            title = "新标题",
            description = "新描述",
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
            dueDate = 20_003L,
        )

        val after = repository.getTask(id)!!
        assertEquals("新标题", after.title)
        assertEquals("新描述", after.description)
        assertEquals(TaskPriority.HIGH, after.priority)
        assertEquals(TaskCategory.WORK, after.category)
        assertEquals(20_003L, after.dueDate)
        assertTrue(after.isCompleted)
        assertEquals(before.completedAt, after.completedAt)
        assertEquals(before.createdAt, after.createdAt)
    }

    @Test
    fun blankTitleIsRejectedBeforeReachingRoom() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createTask(
                    title = "   ",
                    description = null,
                    priority = TaskPriority.MEDIUM,
                    category = TaskCategory.OTHER,
                    dueDate = null,
                )
            }
        }
        runBlocking {
            assertEquals(0, repository.observeTasks().first().size)
        }
    }

    @Test
    fun fileBackedDatabaseSurvivesCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "orbit-persistence-test.db"
        context.deleteDatabase(databaseName)

        var fileDatabase = Room.databaseBuilder(
            context,
            OrbitDatabase::class.java,
            databaseName,
        ).build()
        var fileRepository = TaskRepository(fileDatabase)

        val id = fileRepository.createTask(
            title = "重启后仍在",
            description = "持久化验收",
            priority = TaskPriority.HIGH,
            category = TaskCategory.WORK,
            dueDate = 20_000L,
        )
        fileRepository.setCompleted(id, true)
        fileDatabase.close()

        fileDatabase = Room.databaseBuilder(
            context,
            OrbitDatabase::class.java,
            databaseName,
        ).build()
        fileRepository = TaskRepository(fileDatabase)

        val restored = fileRepository.getTask(id)
        assertNotNull(restored)
        assertEquals("重启后仍在", restored!!.title)
        assertEquals("持久化验收", restored.description)
        assertTrue(restored.isCompleted)
        assertNotNull(restored.completedAt)

        fileDatabase.close()
        context.deleteDatabase(databaseName)
        Unit
    }
}
