package com.noop.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Whoop5CommandPolicyTest {

    @Test
    fun allowsOnlyClockLiveHapticsAndHistoricalCommands() {
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.SET_CLOCK))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.GET_CLOCK))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.TOGGLE_REALTIME_HR))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.RUN_HAPTICS_PATTERN))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.GET_DATA_RANGE))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.SEND_HISTORICAL_DATA))
        assertTrue(Whoop5CommandPolicy.allows(CommandNumber.HISTORICAL_DATA_RESULT))

        assertFalse(Whoop5CommandPolicy.allows(CommandNumber.GET_BATTERY_LEVEL))
        assertFalse(Whoop5CommandPolicy.allows(CommandNumber.SET_ALARM_TIME))
        assertFalse(Whoop5CommandPolicy.allows(CommandNumber.SELECT_WRIST))
    }

    @Test
    fun gen5HistoricalRequestCommandsUseEmptyPayloads() {
        assertArrayEquals(
            byteArrayOf(),
            Whoop5CommandPolicy.payloadFor(CommandNumber.GET_DATA_RANGE, byteArrayOf(0)),
        )
        assertArrayEquals(
            byteArrayOf(),
            Whoop5CommandPolicy.payloadFor(CommandNumber.SEND_HISTORICAL_DATA, byteArrayOf(0)),
        )
    }

    @Test
    fun gen5ClockCommandsUseExpectedPayloads() {
        val setClock = byteArrayOf(1, 2, 3, 4, 0, 0, 0, 0)

        assertArrayEquals(
            setClock,
            Whoop5CommandPolicy.payloadFor(CommandNumber.SET_CLOCK, setClock),
        )
        assertArrayEquals(
            byteArrayOf(),
            Whoop5CommandPolicy.payloadFor(CommandNumber.GET_CLOCK, byteArrayOf(0)),
        )
    }

    @Test
    fun gen5HistoricalAckPreservesHistoryEndPayload() {
        val ack = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        assertArrayEquals(
            ack,
            Whoop5CommandPolicy.payloadFor(CommandNumber.HISTORICAL_DATA_RESULT, ack),
        )
    }

    @Test
    fun disallowedCommandHasNoPayload() {
        assertNull(Whoop5CommandPolicy.payloadFor(CommandNumber.GET_BATTERY_LEVEL, byteArrayOf(0)))
    }
}
