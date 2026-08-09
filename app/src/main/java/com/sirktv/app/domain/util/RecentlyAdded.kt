package com.sirktv.app.domain.util

/** Shared "Recently Added" ranking used by Home, Movies, and Series. */
object RecentlyAdded {

    const val NEW_BADGE_WINDOW_MS = 30L * 24 * 60 * 60 * 1000
    const val DEFAULT_LIMIT = 20

    /**
     * Ranks [items] most-recently-added first. Primary key is the real
     * Xtream "added"/"last_modified" timestamp; when that's missing (0 or
     * absent from the provider's response) items fall back to [fallbackKey]
     * — pass the numeric stream/series ID, since higher IDs are assigned to
     * more recently added catalog entries — so the ranking degrades
     * gracefully instead of collapsing to whatever order the DB happened to
     * return rows in.
     *
     * If any items were added within the last 30 days, only those are
     * returned (so the row reads as genuinely new). If none were — e.g. the
     * whole catalog was synced once and nothing has been added since — the
     * window is dropped and the [limit] most-recently-ranked items are
     * returned regardless of age, so the row is never empty just because
     * the provider's catalog is older than 30 days.
     */
    fun <T> select(
        items: List<T>,
        limit: Int = DEFAULT_LIMIT,
        addedAtEpochMillis: (T) -> Long,
        fallbackKey: (T) -> Long,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): List<T> {
        val ranked = items.sortedWith(
            compareByDescending<T> { addedAtEpochMillis(it) }.thenByDescending { fallbackKey(it) }
        )
        val withinWindow = ranked.filter { isWithinNewWindow(addedAtEpochMillis(it), nowEpochMillis) }
        return (withinWindow.ifEmpty { ranked }).take(limit)
    }

    /** Whether an item added at [addedAtEpochMillis] still qualifies for the NEW badge. */
    fun isWithinNewWindow(addedAtEpochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        addedAtEpochMillis > 0L && nowEpochMillis - addedAtEpochMillis in 0..NEW_BADGE_WINDOW_MS
}
