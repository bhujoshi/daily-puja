package com.worship.nityamandir.data

/**
 * Pure selection persistence and sync rules so bundled free or already-unlocked
 * shrine choices apply reliably even when offline or ahead of the remote catalog.
 */
internal object ShrineSelectionSync {
    fun selectionPreferenceKey(accountId: String?): String? =
        accountId?.takeIf { it.isNotBlank() }?.let { "account_selection_$it" }

    fun unsyncedPreferenceKey(accountId: String?): String? =
        accountId?.takeIf { it.isNotBlank() }?.let { "account_selection_unsynced_$it" }

    fun retryPreferenceKey(accountId: String?): String? =
        accountId?.takeIf { it.isNotBlank() }?.let { "account_selection_retry_$it" }

    fun ownsSelection(
        selection: ShrineSelection,
        unlockedPackage: Boolean = false,
        ownedKeys: Set<String> = emptySet(),
        coinCosts: Map<String, Int> = emptyMap()
    ): Boolean = selection.values.all { (category, value) ->
        val items = value.split(',').filter { it.isNotBlank() }
        items.isNotEmpty() && items.all { item ->
            ShrineUnlocks.cost(category, item, coinCosts[category]) == 0 ||
                unlockedPackage ||
                "$category:$item" in ownedKeys
        }
    }

    fun resolveSelection(
        signedIn: Boolean,
        unsyncedLocalSelection: Map<String, String>?,
        isUnsynced: Boolean,
        profileSelection: Map<String, String>?,
        guestSelection: Map<String, String>?,
        unlockedPackage: Boolean = false,
        ownedKeys: Set<String> = emptySet(),
        coinCosts: Map<String, Int> = emptyMap()
    ): ShrineSelection {
        if (signedIn && isUnsynced && !unsyncedLocalSelection.isNullOrEmpty()) {
            val candidate = ShrineSelection(unsyncedLocalSelection)
            if (ownsSelection(candidate, unlockedPackage, ownedKeys, coinCosts)) {
                return candidate
            }
        }
        if (profileSelection != null) {
            return ShrineSelection(profileSelection)
        }
        return ShrineSelection(guestSelection.orEmpty())
    }

    enum class RemoteSaveOutcome {
        SAVED_FOR_ACCOUNT_RETRY_LATER,
        SAVED_FOR_ACCOUNT_CATALOG_AHEAD,
        SAVED_AS_GUEST_AFTER_SIGN_OUT,
        RETHROW_SIGN_IN_REQUIRED
    }

    fun onRemoteSaveFailure(
        stillSignedIn: Boolean,
        isConnectionFailure: Boolean,
        selection: ShrineSelection
    ): RemoteSaveOutcome = when {
        stillSignedIn && isConnectionFailure -> RemoteSaveOutcome.SAVED_FOR_ACCOUNT_RETRY_LATER
        stillSignedIn -> RemoteSaveOutcome.SAVED_FOR_ACCOUNT_CATALOG_AHEAD
        ownsSelection(selection) -> RemoteSaveOutcome.SAVED_AS_GUEST_AFTER_SIGN_OUT
        else -> RemoteSaveOutcome.RETHROW_SIGN_IN_REQUIRED
    }
}
