package com.worship.nityamandir

import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.data.ShrineSelectionSync
import org.junit.Assert.*
import org.junit.Test

class ShrineSelectionSyncTest {
    private val divyaDarshanDraft = ShrineSelection(
        mapOf(
            "shrine" to "divya_darshan",
            "idols" to "original",
            "garland" to "original",
            "flowers" to "original",
            "incense" to "original",
            "lamp" to "original",
            "shankh" to "original",
            "aarti" to "original",
            "prasad" to "original"
        )
    )

    @Test fun allBundledFreeTemplesAreOwnedWithoutUnlock() {
        for (shrine in listOf("marble", "diwali", "divya_darshan", "kamal_peeth", "swarna_peeth")) {
            val selection = divyaDarshanDraft.with("shrine", shrine)
            assertTrue("Expected $shrine to be owned without purchase", ShrineSelectionSync.ownsSelection(selection))
        }
    }

    @Test fun paidItemsRequireOwnershipOrPackageEntitlement() {
        val ivorySelection = divyaDarshanDraft.with("shrine", "ivory")
        assertFalse(ShrineSelectionSync.ownsSelection(ivorySelection))
        assertTrue(ShrineSelectionSync.ownsSelection(ivorySelection, ownedKeys = setOf("shrine:ivory")))
        assertTrue(ShrineSelectionSync.ownsSelection(ivorySelection, unlockedPackage = true))

        val multiFlower = divyaDarshanDraft.with("flowers", "original,rose")
        assertFalse(ShrineSelectionSync.ownsSelection(multiFlower))
        assertTrue(ShrineSelectionSync.ownsSelection(multiFlower, ownedKeys = setOf("flowers:rose")))
    }

    @Test fun unsyncedLocalSelectionSurvivesServerProfileRefreshWithOlderCatalog() {
        val resolved = ShrineSelectionSync.resolveSelection(
            signedIn = true,
            unsyncedLocalSelection = divyaDarshanDraft.values,
            isUnsynced = true,
            profileSelection = mapOf("shrine" to "marble"),
            guestSelection = mapOf("shrine" to "marble")
        )
        assertEquals("divya_darshan", resolved["shrine"])
        assertEquals("original", resolved["incense"])
    }

    @Test fun syncedServerProfileTakesPrecedenceWhenNotUnsynced() {
        val resolved = ShrineSelectionSync.resolveSelection(
            signedIn = true,
            unsyncedLocalSelection = divyaDarshanDraft.values,
            isUnsynced = false,
            profileSelection = mapOf("shrine" to "kamal_peeth"),
            guestSelection = mapOf("shrine" to "marble")
        )
        assertEquals("kamal_peeth", resolved["shrine"])
    }

    @Test fun remoteSaveFailureKeepsSignedInSelectionWithoutBlockingApply() {
        assertEquals(
            ShrineSelectionSync.RemoteSaveOutcome.SAVED_FOR_ACCOUNT_CATALOG_AHEAD,
            ShrineSelectionSync.onRemoteSaveFailure(
                stillSignedIn = true,
                isConnectionFailure = false,
                selection = divyaDarshanDraft
            )
        )
        assertEquals(
            ShrineSelectionSync.RemoteSaveOutcome.SAVED_FOR_ACCOUNT_RETRY_LATER,
            ShrineSelectionSync.onRemoteSaveFailure(
                stillSignedIn = true,
                isConnectionFailure = true,
                selection = divyaDarshanDraft
            )
        )
        assertEquals(
            ShrineSelectionSync.RemoteSaveOutcome.SAVED_AS_GUEST_AFTER_SIGN_OUT,
            ShrineSelectionSync.onRemoteSaveFailure(
                stillSignedIn = false,
                isConnectionFailure = false,
                selection = divyaDarshanDraft
            )
        )
        assertEquals(
            ShrineSelectionSync.RemoteSaveOutcome.RETHROW_SIGN_IN_REQUIRED,
            ShrineSelectionSync.onRemoteSaveFailure(
                stillSignedIn = false,
                isConnectionFailure = false,
                selection = divyaDarshanDraft.with("shrine", "ivory")
            )
        )
    }

    @Test fun preferenceKeysIsolateAccounts() {
        assertNull(ShrineSelectionSync.selectionPreferenceKey(null))
        assertNull(ShrineSelectionSync.selectionPreferenceKey(" "))
        assertNotEquals(
            ShrineSelectionSync.selectionPreferenceKey("acct-1"),
            ShrineSelectionSync.selectionPreferenceKey("acct-2")
        )
        assertNotEquals(
            ShrineSelectionSync.unsyncedPreferenceKey("acct-1"),
            ShrineSelectionSync.retryPreferenceKey("acct-1")
        )
    }
}
