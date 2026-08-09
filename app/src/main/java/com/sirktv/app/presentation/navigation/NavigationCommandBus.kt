package com.sirktv.app.presentation.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NavigationCommand {
    /** Fire TV remote SEARCH/mic button, or a voice ACTION_SEARCH intent with a pre-filled query. */
    data class OpenSearch(val query: String = "") : NavigationCommand

    /** KEYCODE_MENU pressed with no player screen active — SirKTVNavHost resolves this against the current route: Settings goes back, anywhere else opens Settings. */
    data object MenuPressed : NavigationCommand
}

/**
 * MainActivity.onKeyDown/onNewIntent run outside Compose and have no
 * NavHostController of their own — it's created inside SirKTVNavHost's
 * composition — so commands posted here are collected by SirKTVNavHost and
 * turned into real navigation calls.
 */
@Singleton
class NavigationCommandBus @Inject constructor() {
    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    fun send(command: NavigationCommand) {
        _commands.tryEmit(command)
    }
}
