package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import com.example.ui.viewmodel.WorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RestTimerLogicTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutRepository
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("test_timer_prefs", Context.MODE_PRIVATE)
        val secureStorage = com.example.data.security.SecurePasswordStorage(context)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepository(context, database)
        viewModel = WorkoutViewModel(repository, prefs, secureStorage, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `timer starts, pauses, resumes and dismisses accurately`() = runTest(testDispatcher) {
        // Start timer with 90 seconds
        viewModel.startRestTimer(90, "Przerwa po przysiadach")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isRestTimerActive.value)
        assertFalse(viewModel.isRestTimerPaused.value)
        assertEquals(90, viewModel.restTimerSeconds.value)
        assertEquals(90, viewModel.restTimerRemainingSeconds.value)
        assertEquals("Przerwa po przysiadach", viewModel.restTimerLabel.value)

        // Advance simulated system clock by 10 seconds
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10))
        testDispatcher.scheduler.advanceTimeBy(1000)
        assertEquals(80, viewModel.restTimerRemainingSeconds.value)

        // Pause timer
        viewModel.pauseResumeRestTimer()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isRestTimerPaused.value)
        val pausedSeconds = viewModel.restTimerRemainingSeconds.value
        assertEquals(80, pausedSeconds)

        // While paused, advance clock by 30 seconds - remaining seconds must not decrease
        ShadowSystemClock.advanceBy(Duration.ofSeconds(30))
        testDispatcher.scheduler.advanceTimeBy(1000)
        assertEquals(pausedSeconds, viewModel.restTimerRemainingSeconds.value)

        // Resume timer
        viewModel.pauseResumeRestTimer()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isRestTimerPaused.value)

        // Advance clock by another 5 seconds after resume
        ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
        testDispatcher.scheduler.advanceTimeBy(1000)
        assertEquals(75, viewModel.restTimerRemainingSeconds.value)

        // Dismiss / Cancel timer
        viewModel.dismissRestTimer()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isRestTimerActive.value)
        assertEquals(0, viewModel.restTimerRemainingSeconds.value)
    }

    @Test
    fun `timer modifies duration via add and reduce seconds`() = runTest(testDispatcher) {
        viewModel.startRestTimer(60, "Przerwa")
        testDispatcher.scheduler.advanceUntilIdle()

        // Add 30 seconds (+30s)
        viewModel.addRestSeconds(30)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, viewModel.restTimerRemainingSeconds.value)
        assertEquals(90, viewModel.restTimerSeconds.value)

        // Reduce 15 seconds (-15s)
        viewModel.reduceRestSeconds(15)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(75, viewModel.restTimerRemainingSeconds.value)
    }

    @Test
    fun `timer finishes exactly once after target elapsed duration and handles background return`() = runTest(testDispatcher) {
        viewModel.startRestTimer(30, "Krótka przerwa")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isRestTimerActive.value)

        // Simulate returning to the app after remaining duration has completely elapsed in background
        ShadowSystemClock.advanceBy(Duration.ofSeconds(35))
        testDispatcher.scheduler.advanceTimeBy(1000)

        // Timer must be completed and inactive
        assertFalse(viewModel.isRestTimerActive.value)
        assertEquals(0, viewModel.restTimerRemainingSeconds.value)

        // Advancing time further must not cause crash or repeated notifications
        ShadowSystemClock.advanceBy(Duration.ofSeconds(60))
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertFalse(viewModel.isRestTimerActive.value)
    }

    @Test
    fun `timer settings respect disabled vibration and sound`() {
        // Turn off sound and vibration
        viewModel.setTimerVibrationEnabled(false)
        viewModel.setTimerSoundEnabled(false)

        assertFalse(viewModel.timerVibrationEnabled.value)
        assertFalse(viewModel.timerSoundEnabled.value)

        // Turn them on
        viewModel.setTimerVibrationEnabled(true)
        viewModel.setTimerSoundEnabled(true)

        assertTrue(viewModel.timerVibrationEnabled.value)
        assertTrue(viewModel.timerSoundEnabled.value)
    }
}
