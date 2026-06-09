package com.noop.protocol

/** Family-aware helpers for routing historical offload frames and preparing HISTORY_END acks. */
object HistoricalFrameClassifier {
    fun isOffloadFrame(frame: ByteArray, family: DeviceFamily): Boolean {
        val typeOffset = if (family == DeviceFamily.WHOOP5) 8 else 4
        val type = if (frame.size > typeOffset) frame[typeOffset].toInt() and 0xFF else return false
        return if (family == DeviceFamily.WHOOP5) {
            when (type) {
                PacketType.HISTORICAL_DATA.rawValue,
                PacketType.HISTORICAL_IMU_DATA_STREAM.rawValue,
                PacketType.METADATA.rawValue,
                PuffinPacketType.PUFFIN_METADATA -> true
                else -> false
            }
        } else {
            when (type) {
                PacketType.HISTORICAL_DATA.rawValue,
                PacketType.EVENT.rawValue,
                PacketType.METADATA.rawValue,
                PacketType.CONSOLE_LOGS.rawValue -> true
                else -> false
            }
        }
    }

    fun historyEndAckData(frame: ByteArray, family: DeviceFamily): ByteArray? {
        val start = if (family == DeviceFamily.WHOOP5) 21 else 17
        val end = start + 8
        if (frame.size < end) return null
        return frame.copyOfRange(start, end)
    }
}
