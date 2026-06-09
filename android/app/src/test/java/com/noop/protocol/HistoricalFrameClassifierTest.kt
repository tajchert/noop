package com.noop.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalFrameClassifierTest {

    private fun historyEndBody(): ByteArray = byteArrayOf(
        0x00, 0xf1.toByte(), 0x53, 0x65, // unix 1700000000
        0x00, 0x00, // subseconds
        0x00, 0x00, 0x00, 0x00, // unknown
        0x40, 0xe2.toByte(), 0x01, 0x00, // trim cursor 123456
        0x09, 0x08, 0x07, 0x06, // remaining end_data bytes
    )

    @Test
    fun identifiesWhoop5HistoricalFramesAtPuffinOffset() {
        val frame = Framing.puffinCommandFrame(
            cmd = MetadataType.HISTORY_END.rawValue,
            seq = 7,
            payload = historyEndBody(),
            type = PacketType.METADATA.rawValue,
        )

        assertTrue(HistoricalFrameClassifier.isOffloadFrame(frame, DeviceFamily.WHOOP5))
        assertFalse(HistoricalFrameClassifier.isOffloadFrame(frame, DeviceFamily.WHOOP4))
    }

    @Test
    fun whoop5LiveEventsAreNotHistoricalProgress() {
        val frame = Framing.puffinCommandFrame(
            cmd = EventNumber.WRIST_ON.rawValue,
            seq = 7,
            payload = byteArrayOf(0, 0, 0, 0),
            type = PacketType.EVENT.rawValue,
        )

        assertFalse(HistoricalFrameClassifier.isOffloadFrame(frame, DeviceFamily.WHOOP5))
    }

    @Test
    fun whoop5MetadataParsesAndClassifiesHistoryEnd() {
        val frame = Framing.puffinCommandFrame(
            cmd = MetadataType.HISTORY_END.rawValue,
            seq = 7,
            payload = historyEndBody(),
            type = PacketType.METADATA.rawValue,
        )

        val parsed = Framing.parseFrame(frame, DeviceFamily.WHOOP5)
        assertEquals("METADATA", parsed.typeName)
        assertEquals("HISTORY_END(2)", parsed.parsed["meta_type"])
        assertEquals(1_700_000_000, parsed.parsed["unix"])
        assertEquals(123_456, parsed.parsed["trim_cursor"])
        assertTrue(classifyHistoricalMeta(parsed) is HistoricalMeta.End)
    }

    @Test
    fun extractsWhoop5HistoryEndAckDataAtPuffinOffset() {
        val frame = Framing.puffinCommandFrame(
            cmd = MetadataType.HISTORY_END.rawValue,
            seq = 7,
            payload = historyEndBody(),
            type = PacketType.METADATA.rawValue,
        )

        assertArrayEquals(
            byteArrayOf(0x40, 0xe2.toByte(), 0x01, 0x00, 0x09, 0x08, 0x07, 0x06),
            HistoricalFrameClassifier.historyEndAckData(frame, DeviceFamily.WHOOP5),
        )
    }

    @Test
    fun whoop5EventDecodesAtPuffinOffset() {
        val eventBody = byteArrayOf(
            0x00, // filler at offset 11; event timestamp starts at offset 12
            0x00, 0xf1.toByte(), 0x53, 0x65, // event timestamp 1700000000
        )
        val frame = Framing.puffinCommandFrame(
            cmd = EventNumber.WRIST_ON.rawValue,
            seq = 7,
            payload = eventBody,
            type = PacketType.EVENT.rawValue,
        )

        val parsed = Framing.parseFrame(frame, DeviceFamily.WHOOP5)

        assertEquals("EVENT", parsed.typeName)
        assertEquals("WRIST_ON(9)", parsed.parsed["event"])
        assertEquals(1_700_000_000, parsed.parsed["event_timestamp"])
    }
}
