package com.noop.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalSyncStartPolicyTest {

    @Test
    fun startsOnlyWhenConnectedBondedReadyAndIdle() {
        assertTrue(
            HistoricalSyncStartPolicy.shouldRequestSync(
                connected = true,
                bonded = true,
                sessionReady = true,
                backfilling = false,
            ),
        )
    }

    @Test
    fun doesNotStartBeforeSessionIsReady() {
        assertFalse(
            HistoricalSyncStartPolicy.shouldRequestSync(
                connected = true,
                bonded = true,
                sessionReady = false,
                backfilling = false,
            ),
        )
    }

    @Test
    fun doesNotStartWhileBackfilling() {
        assertFalse(
            HistoricalSyncStartPolicy.shouldRequestSync(
                connected = true,
                bonded = true,
                sessionReady = true,
                backfilling = true,
            ),
        )
    }
}
