package com.noop.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionSubscriptionPolicyTest {

    @Test
    fun whoop5SendsClientHelloBeforeNotifications() {
        assertFalse(ConnectionSubscriptionPolicy.queueFamilyNotificationsBeforeSession(DeviceFamily.WHOOP5))
    }

    @Test
    fun whoop5DoesNotQueueStandardProfilesBeforeClientHello() {
        assertFalse(ConnectionSubscriptionPolicy.queueStandardProfilesBeforeSession(DeviceFamily.WHOOP5))
    }

    @Test
    fun whoop4KeepsExistingPreSessionSubscriptions() {
        assertTrue(ConnectionSubscriptionPolicy.queueFamilyNotificationsBeforeSession(DeviceFamily.WHOOP4))
        assertTrue(ConnectionSubscriptionPolicy.queueStandardProfilesBeforeSession(DeviceFamily.WHOOP4))
    }

    @Test
    fun whoop5WaitsBrieflyBeforeFirstSessionWrite() {
        assertTrue(ConnectionSubscriptionPolicy.sessionStartDelayMs(DeviceFamily.WHOOP5) > 0)
        assertTrue(ConnectionSubscriptionPolicy.sessionStartDelayMs(DeviceFamily.WHOOP4) == 0L)
    }

    @Test
    fun whoop5PrefersDirectConnectWhenAlreadyBonded() {
        assertTrue(ConnectionSubscriptionPolicy.preferBondedDirectConnect(DeviceFamily.WHOOP5))
        assertFalse(ConnectionSubscriptionPolicy.preferBondedDirectConnect(DeviceFamily.WHOOP4))
    }

    @Test
    fun whoop5WaitsForRangeSuccessBeforeHistoricalTransfer() {
        assertTrue(ConnectionSubscriptionPolicy.waitForDataRangeSuccessBeforeHistoricalTransfer(DeviceFamily.WHOOP5))
        assertFalse(ConnectionSubscriptionPolicy.waitForDataRangeSuccessBeforeHistoricalTransfer(DeviceFamily.WHOOP4))
    }

    @Test
    fun whoop5AcksHistoryEndForMetadataOnlyAndBodyTransfers() {
        assertTrue(ConnectionSubscriptionPolicy.shouldAckHistoricalTrim(DeviceFamily.WHOOP5, bodyPacketsSeen = 0))
        assertTrue(ConnectionSubscriptionPolicy.shouldAckHistoricalTrim(DeviceFamily.WHOOP5, bodyPacketsSeen = 1))
        assertTrue(ConnectionSubscriptionPolicy.shouldAckHistoricalTrim(DeviceFamily.WHOOP4, bodyPacketsSeen = 0))
    }
}
