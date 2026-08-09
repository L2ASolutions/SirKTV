package com.sirktv.app.presentation.player

import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a weak reference to whichever [ActivePlayerHandle] is currently on
 * screen. Weak so a ViewModel that forgets to [clear] on teardown can't leak
 * — the next screen's [register] simply overwrites it, and a stale reference
 * that outlives its ViewModel just returns null instead of resurrecting it.
 */
@Singleton
class ActivePlayerState @Inject constructor() {
    private var handleRef: WeakReference<ActivePlayerHandle>? = null

    val current: ActivePlayerHandle?
        get() = handleRef?.get()

    fun register(handle: ActivePlayerHandle) {
        handleRef = WeakReference(handle)
    }

    /** No-ops unless [handle] is still the currently registered one, so an out-of-order teardown never clears a newer screen's handle. */
    fun clear(handle: ActivePlayerHandle) {
        if (handleRef?.get() === handle) handleRef = null
    }
}
