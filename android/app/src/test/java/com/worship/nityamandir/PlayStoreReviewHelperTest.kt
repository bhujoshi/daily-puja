package com.worship.nityamandir

import com.worship.nityamandir.data.PlayStoreReviewHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStoreReviewHelperTest {
    @Test fun reviewPromptShowsAfterFirstCompletedPujaUntilClicked() {
        assertFalse(PlayStoreReviewHelper.shouldShowPostPujaReviewPrompt(completedPujas = 0, reviewAlreadyClicked = false))
        assertTrue(PlayStoreReviewHelper.shouldShowPostPujaReviewPrompt(completedPujas = 1, reviewAlreadyClicked = false))
        assertTrue(PlayStoreReviewHelper.shouldShowPostPujaReviewPrompt(completedPujas = 5, reviewAlreadyClicked = false))
        assertFalse(PlayStoreReviewHelper.shouldShowPostPujaReviewPrompt(completedPujas = 1, reviewAlreadyClicked = true))
        assertFalse(PlayStoreReviewHelper.shouldShowPostPujaReviewPrompt(completedPujas = 5, reviewAlreadyClicked = true))
    }
}
