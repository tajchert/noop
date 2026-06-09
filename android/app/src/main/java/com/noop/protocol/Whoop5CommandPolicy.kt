package com.noop.protocol

/**
 * Narrow allowlist for commands NOOP may send on the WHOOP 5.0/MG puffin transport.
 *
 * The 5/MG command surface is intentionally smaller than WHOOP 4.0 while hardware support is being
 * validated. Historical request commands use empty payloads, matching the Goose/OpenWhoop Gen5
 * historical-sync flow; HISTORY_END acks preserve the exact ack payload built from strap metadata.
 */
object Whoop5CommandPolicy {
    private val allowedCommands = setOf(
        CommandNumber.TOGGLE_REALTIME_HR,
        CommandNumber.RUN_HAPTICS_PATTERN,
        CommandNumber.GET_DATA_RANGE,
        CommandNumber.SEND_HISTORICAL_DATA,
        CommandNumber.HISTORICAL_DATA_RESULT,
    )

    fun allows(cmd: CommandNumber): Boolean = cmd in allowedCommands

    fun payloadFor(cmd: CommandNumber, requested: ByteArray): ByteArray? {
        if (!allows(cmd)) return null
        return when (cmd) {
            CommandNumber.GET_DATA_RANGE,
            CommandNumber.SEND_HISTORICAL_DATA -> byteArrayOf()
            else -> requested
        }
    }
}
