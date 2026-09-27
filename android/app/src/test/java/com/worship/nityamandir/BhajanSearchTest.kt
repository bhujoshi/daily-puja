package com.worship.nityamandir

import com.worship.nityamandir.engine.BhajanSearch
import org.junit.Assert.*
import org.junit.Test

class BhajanSearchTest {
    @Test fun spellingVariantSearchesBeyondDevotionalTags() {
        val query = BhajanSearch.query("hanuman chalisha")
        assertTrue(query.contains("\"chalisa\" OR \"chalisha\""))
        assertTrue(query.contains("title:"))
        assertTrue(query.contains("subject:(bhajan"))
        assertEquals(query, BhajanSearch.query("  Hanuman Chalisa  "))
    }
    @Test fun deityFilterCombinesAliasesWithDevotionalConstraint() {
        val query = BhajanSearch.query("", "shiva shiv शिव")
        assertTrue(query.contains("subject:(bhajan"))
        assertTrue(query.contains("title:(\"shiva\" OR \"shiv\" OR \"शिव\")"))
    }
    @Test fun preservesHindiVowelMarks() {
        val query = BhajanSearch.query("हनुमान चालीसा")
        assertTrue(query.contains("\"हनुमान\""))
        assertTrue(query.contains("\"चालीसा\""))
    }
    @Test fun blankInputBrowsesDevotionalAudio() {
        assertTrue(BhajanSearch.query("  ").contains("subject:(bhajan"))
    }
    @Test fun querySyntaxCannotEscapeAudioFilter() {
        val query = BhajanSearch.query("ganesh\") OR mediatype:movies")
        assertFalse(query.contains("mediatype:movies"))
        assertTrue(query.startsWith("mediatype:audio AND"))
        assertTrue(query.contains("\"or\""))
    }
}
