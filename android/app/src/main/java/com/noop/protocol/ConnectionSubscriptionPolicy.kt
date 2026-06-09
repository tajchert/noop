package com.noop.protocol

object ConnectionSubscriptionPolicy {
    fun queueFamilyNotificationsBeforeSession(family: DeviceFamily): Boolean =
        when (family) {
            DeviceFamily.WHOOP4 -> true
            DeviceFamily.WHOOP5 -> false
        }

    fun queueStandardProfilesBeforeSession(family: DeviceFamily): Boolean =
        when (family) {
            DeviceFamily.WHOOP4 -> true
            DeviceFamily.WHOOP5 -> false
        }

    fun sessionStartDelayMs(family: DeviceFamily): Long =
        when (family) {
            DeviceFamily.WHOOP4 -> 0L
            DeviceFamily.WHOOP5 -> 750L
        }

    fun preferBondedDirectConnect(family: DeviceFamily): Boolean =
        when (family) {
            DeviceFamily.WHOOP4 -> false
            DeviceFamily.WHOOP5 -> true
        }

    fun waitForDataRangeSuccessBeforeHistoricalTransfer(family: DeviceFamily): Boolean =
        when (family) {
            DeviceFamily.WHOOP4 -> false
            DeviceFamily.WHOOP5 -> true
        }

    fun shouldAckHistoricalTrim(family: DeviceFamily, bodyPacketsSeen: Int): Boolean =
        when (family) {
            DeviceFamily.WHOOP4 -> true
            DeviceFamily.WHOOP5 -> bodyPacketsSeen == 0
        }
}
