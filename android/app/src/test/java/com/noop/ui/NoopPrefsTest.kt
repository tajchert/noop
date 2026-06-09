package com.noop.ui

import com.noop.ble.WhoopModel
import org.junit.Assert.assertEquals
import org.junit.Test

class NoopPrefsTest {

    @Test
    fun mapsPersistedWhoopModelTokenToSelection() {
        assertEquals(
            WhoopModel.WHOOP5_MG,
            NoopPrefs.whoopModelFromPref("WHOOP5_MG"),
        )
        assertEquals(
            WhoopModel.WHOOP4,
            NoopPrefs.whoopModelFromPref("WHOOP4"),
        )
    }

    @Test
    fun fallsBackToWhoop4ForMissingOrUnknownModelToken() {
        assertEquals(WhoopModel.WHOOP4, NoopPrefs.whoopModelFromPref(null))
        assertEquals(WhoopModel.WHOOP4, NoopPrefs.whoopModelFromPref("WHOOP_6"))
    }

    @Test
    fun storesStableWhoopModelToken() {
        assertEquals("WHOOP5_MG", NoopPrefs.whoopModelPrefValue(WhoopModel.WHOOP5_MG))
        assertEquals("WHOOP4", NoopPrefs.whoopModelPrefValue(WhoopModel.WHOOP4))
    }
}
