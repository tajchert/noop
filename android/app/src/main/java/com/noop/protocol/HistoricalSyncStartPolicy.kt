package com.noop.protocol

/** Shared gate for kicking a historical offload session. */
object HistoricalSyncStartPolicy {
    fun shouldRequestSync(
        connected: Boolean,
        bonded: Boolean,
        sessionReady: Boolean,
        backfilling: Boolean,
    ): Boolean = connected && bonded && sessionReady && !backfilling
}
