package com.sirktv.app.presentation.home

import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.testutil.FakeDisplayNameRepository
import com.sirktv.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Home is a pure 5-tile launcher now — the only state it exposes is the
 * greeting name, sourced from [CurrentSession]. No Room reads, no API calls.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `display name reflects the current session's greeting name`() = runTest {
        // greetingName is stateIn(appScope, SharingStarted.Eagerly, null) — its
        // combine() needs the scope's dispatcher to actually run once before
        // the real value replaces the null default, so the scope's dispatcher
        // must be tied to this test's own scheduler and explicitly advanced.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appScope = CoroutineScope(dispatcher)
        val currentSession = CurrentSession(FakeDisplayNameRepository("Kay"), appScope)
        val viewModel = HomeViewModel(currentSession)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Kay", viewModel.displayName.value)
    }
}
