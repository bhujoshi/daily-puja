package com.worship.nityamandir

import com.worship.nityamandir.engine.BhajanPlayer
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

    @Test fun matchesTrackHandlesHindiAndSpellingAliases() {
        assertTrue(
            BhajanSearch.matchesTrack(
                title = "Shree Hanuman Chalisa",
                artist = "Hariharan",
                deity = "hanuman",
                collection = "chalisa",
                rawQuery = "hanuman chalisha"
            )
        )
        assertTrue(
            BhajanSearch.matchesTrack(
                title = "Shree Hanuman Chalisa",
                artist = "Hariharan",
                deity = "hanuman",
                collection = "chalisa",
                rawQuery = "हनुमान चालीसा"
            )
        )
        assertTrue(
            BhajanSearch.matchesTrack(
                title = "Shree Hanuman Chalisa",
                artist = "Hariharan",
                deity = "hanuman",
                collection = "chalisa",
                rawQuery = "bajrang"
            )
        )
        assertFalse(
            BhajanSearch.matchesTrack(
                title = "Shree Hanuman Chalisa",
                artist = "Hariharan",
                deity = "hanuman",
                collection = "chalisa",
                rawQuery = "shiv tandav"
            )
        )
    }

    @Test fun parseYoutubeSearchResultsExtractsVideoRenderers() {
        val sampleInnertubeJson = """
            {
              "contents": {
                "twoColumnSearchResultsRenderer": {
                  "primaryContents": {
                    "sectionListRenderer": {
                      "contents": [
                        {
                          "itemSectionRenderer": {
                            "contents": [
                              {
                                "videoRenderer": {
                                  "videoId": "AETFvQonfV8",
                                  "title": {
                                    "runs": [
                                      { "text": "श्री हनुमान चालीसा | Shree Hanuman Chalisa Original Video" }
                                    ]
                                  },
                                  "longBylineText": {
                                    "runs": [
                                      { "text": "T-Series Bhakti Sagar" }
                                    ]
                                  }
                                }
                              },
                              {
                                "videoRenderer": {
                                  "videoId": "hMBKmQEPNzI",
                                  "title": {
                                    "simpleText": "Shiv Tandav Stotram | शंकर महादेवन"
                                  },
                                  "ownerText": {
                                    "runs": [
                                      { "text": "Times Music Spiritual" }
                                    ]
                                  }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val tracks = BhajanSearch.parseYoutubeSearchResults(sampleInnertubeJson, defaultDeity = "mixed")
        assertEquals(2, tracks.size)
        assertEquals("AETFvQonfV8", tracks[0].youtubeId)
        assertTrue(tracks[0].artist.contains("T-Series Bhakti Sagar"))
        assertEquals("hanuman", tracks[0].deity)
        assertEquals("hanuman", tracks[0].collection)

        assertEquals("hMBKmQEPNzI", tracks[1].youtubeId)
        assertEquals("shiva", tracks[1].deity)
        assertEquals("shiva", tracks[1].collection)
        assertTrue(tracks[1].isYoutubeOnly)
    }

    @Test fun relevantRecommendationsPrioritizeSameDeityAndInterleaveSources() {
        val anchorShiva = BhajanPlayer.Track(
            title = "Om Namah Shivay",
            url = "https://archive.org/download/shiv1.mp3",
            id = "mandir_shiv_1",
            deity = "shiva",
            collection = "shiva",
            artist = "Hariharan",
            isYoutube = false
        )
        val mandirShiva2 = BhajanPlayer.Track(
            title = "Shiv Tandav Stotram",
            url = "https://archive.org/download/shiv2.mp3",
            id = "mandir_shiv_2",
            deity = "shiva",
            collection = "shiva",
            artist = "Hariharan",
            isYoutube = false
        )
        val mandirDurga = BhajanPlayer.Track(
            title = "Ambe Tu Hai Jagdambe Kali",
            url = "https://archive.org/download/durga1.mp3",
            id = "mandir_durga_1",
            deity = "durga",
            collection = "durga",
            artist = "Anuradha Paudwal",
            isYoutube = false
        )
        val ytShiva = BhajanPlayer.Track(
            title = "Mera Bhola Hai Bhandari",
            url = "https://archive.org/download/shiv_yt.mp3#yt",
            id = "yt_shiv_1",
            deity = "shiva",
            collection = "shiva",
            artist = "Hansraj Raghuwanshi",
            youtubeId = "gaJR15qWTDA",
            isYoutube = true
        )
        val ytKrishna = BhajanPlayer.Track(
            title = "Mere Banke Bihari Laal",
            url = "https://archive.org/download/krishna_yt.mp3#yt",
            id = "yt_krishna_1",
            deity = "krishna",
            collection = "mixed",
            artist = "Lakhbir Singh Lakkha",
            youtubeId = "L1s4jaR0TpM",
            isYoutube = true
        )

        val catalog = listOf(anchorShiva, mandirDurga, mandirShiva2, ytKrishna, ytShiva)
        val relevant = BhajanSearch.relevantRecommendations(
            anchor = anchorShiva,
            catalog = catalog,
            youtubeResults = emptyList(),
            searchQuery = "",
            idol = "original"
        )

        // Anchor itself is excluded from the recommendations list
        assertFalse(relevant.any { it.id == anchorShiva.id })
        // Top Mandir and top YouTube recommendations are both Shiva tracks, interleaved
        assertEquals("mandir_shiv_2", relevant[0].id)
        assertFalse(relevant[0].isYoutubeOnly)
        assertEquals("yt_shiv_1", relevant[1].id)
        assertTrue(relevant[1].isYoutubeOnly)
    }

    @Test fun parseYoutubePlayerStreamUrlExtractsDirectAudioStream() {
        val samplePlayerJson = """
            {
              "streamingData": {
                "adaptiveFormats": [
                  {
                    "itag": 140,
                    "url": "https://rr1---sn-ci5gup-qxae.googlevideo.com/videoplayback?expire=1700000000\u0026mime=audio%2Fmp4",
                    "mimeType": "audio/mp4; codecs=\"mp4a.40.2\""
                  }
                ]
              }
            }
        """.trimIndent()
        val url = BhajanSearch.parseYoutubePlayerStreamUrl(samplePlayerJson)
        assertEquals(
            "https://rr1---sn-ci5gup-qxae.googlevideo.com/videoplayback?expire=1700000000&mime=audio%2Fmp4",
            url
        )
    }

    @Test fun recommendedNextWordsSuggestsDevotionalCompletionsLikeYouTube() {
        val enWords = BhajanSearch.recommendedNextWords("hanuman", hindi = false)
        assertTrue(enWords.contains("chalisa"))
        assertTrue(enWords.contains("ashtak"))

        val hiWords = BhajanSearch.recommendedNextWords("हनुमान", hindi = true)
        assertTrue(hiWords.contains("चालीसा"))

        val predictions = BhajanSearch.autocompletePredictions(
            query = "hanuman",
            hindi = false,
            remoteSuggestions = listOf("hanuman lofi bhajan")
        )
        assertTrue(predictions.any { it.equals("hanuman chalisa", ignoreCase = true) })
        assertTrue(predictions.any { it.equals("hanuman lofi bhajan", ignoreCase = true) })
    }

    @Test fun youtubeFeedRecommendationsPrioritizesYoutubeTracksWhenNextSongPlays() {
        val currentTrack = BhajanPlayer.Track(
            title = "Shree Hanuman Chalisa",
            url = "https://archive.org/download/hc.mp3",
            id = "yt_hanuman_chalisa",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "AETFvQonfV8",
            isYoutube = true
        )
        val relatedYtTrack = BhajanPlayer.Track(
            title = "Sankat Mochan Hanuman Ashtak",
            url = "https://www.youtube.com/watch?v=HH_a6aRO1TE",
            id = "yt_sankat_mochan",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "HH_a6aRO1TE",
            isYoutube = true
        )
        val catalogTrackWithYtId = BhajanPlayer.Track(
            title = "Bajrang Baan",
            url = "https://archive.org/download/bb.mp3",
            id = "mandir_bb",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "xyz12345678",
            isYoutube = false
        )

        val feed = BhajanSearch.youtubeFeedRecommendations(
            anchor = currentTrack,
            catalog = listOf(currentTrack, catalogTrackWithYtId),
            youtubeResults = listOf(relatedYtTrack),
            searchQuery = "",
            categoryFilter = "all",
            idol = "original"
        )
        assertEquals(2, feed.size)
        assertEquals("yt_sankat_mochan", feed[0].id)
        assertTrue(feed.all { it.isYoutubeOnly })
    }

    @Test fun searchQueryImmediatelyRefreshesFeedWithMatchingTracks() {
        val playingGanesh = BhajanPlayer.Track(
            title = "Jai Ganesh Deva",
            url = "https://archive.org/download/ganesh.mp3",
            id = "ganesh_1",
            deity = "ganesh",
            collection = "ganesh",
            youtubeId = "87Y-OH_mJxM",
            isYoutube = true
        )
        val shivTrack = BhajanPlayer.Track(
            title = "Shiv Tandav Stotram",
            url = "https://archive.org/download/shiv.mp3",
            id = "shiv_1",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "hMBKmQEPNzI",
            isYoutube = true
        )
        val feed = BhajanSearch.youtubeFeedRecommendations(
            anchor = playingGanesh,
            catalog = listOf(playingGanesh, shivTrack),
            youtubeResults = emptyList(),
            searchQuery = "shiv",
            categoryFilter = "all",
            idol = "original"
        )
        assertEquals(1, feed.size)
        assertEquals("shiv_1", feed[0].id)
    }

    @Test fun recommendationsNeverIncludePlayingSongOrItsDuplicateTwins() {
        val catalogHanumanChalisa = BhajanPlayer.Track(
            title = "Hanuman Chalisa",
            url = "https://archive.org/download/ShriHanumanChalisa_404/Shri-Hanuman-Chalisa.mp3",
            id = "46",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "AETFvQonfV8",
            isYoutube = false
        )
        val featuredTwin = BhajanPlayer.Track(
            title = "Shree Hanuman Chalisa (Original Video)",
            url = "https://archive.org/download/ShriHanumanChalisa_404/Shri-Hanuman-Chalisa.mp3#yt_hanuman_chalisa",
            id = "yt_hanuman_chalisa",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "AETFvQonfV8",
            isYoutube = true
        )
        val liveReuploadTwin = BhajanPlayer.Track(
            title = "श्री हनुमान चालीसा | Shree Hanuman Chalisa Full Audio",
            url = "https://www.youtube.com/watch?v=otherVideo11",
            id = "yt_otherVideo11",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "otherVideo11",
            isYoutube = true
        )
        val sankatMochan = BhajanPlayer.Track(
            title = "Sankat Mochan Hanuman Ashtak",
            url = "https://archive.org/download/sm.mp3",
            id = "47",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "HH_a6aRO1TE",
            isYoutube = false
        )
        val bajrangBaan = BhajanPlayer.Track(
            title = "Bajrang Baan",
            url = "https://archive.org/download/bb.mp3",
            id = "49",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "zRIdNyLRJ6c",
            isYoutube = false
        )

        val feed = BhajanSearch.youtubeFeedRecommendations(
            anchor = catalogHanumanChalisa,
            catalog = listOf(catalogHanumanChalisa, featuredTwin, sankatMochan, bajrangBaan),
            youtubeResults = listOf(liveReuploadTwin),
            searchQuery = "",
            categoryFilter = "all",
            idol = "original"
        )

        assertFalse(feed.any { it.id == "46" })
        assertFalse(feed.any { it.id == "yt_hanuman_chalisa" })
        assertFalse(feed.any { it.id == "yt_otherVideo11" })
        assertEquals(2, feed.size)
    }

    @Test fun recommendationsDynamicallyChangeAcrossTracksAndPlayHistory() {
        val shiv1 = BhajanPlayer.Track(
            title = "Om Namah Shivay",
            url = "https://archive.org/download/s1.mp3",
            id = "s1",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "nVib8eC2JDI",
            isYoutube = true
        )
        val shiv2 = BhajanPlayer.Track(
            title = "Har Har Mahadev",
            url = "https://archive.org/download/s2.mp3",
            id = "s2",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "YHC5gWW0n0s",
            isYoutube = true
        )
        val shiv3 = BhajanPlayer.Track(
            title = "Shiv Tandav Stotram",
            url = "https://archive.org/download/s3.mp3",
            id = "s3",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "hMBKmQEPNzI",
            isYoutube = true
        )
        val shiv4 = BhajanPlayer.Track(
            title = "Mera Bhola Hai Bhandari",
            url = "https://archive.org/download/s4.mp3",
            id = "s4",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "gaJR15qWTDA",
            isYoutube = true
        )
        val shiv5 = BhajanPlayer.Track(
            title = "Karpur Gauram",
            url = "https://archive.org/download/s5.mp3",
            id = "s5",
            deity = "shiva",
            collection = "shiva",
            youtubeId = "k_ZGo8qEcOA",
            isYoutube = true
        )
        val catalog = listOf(shiv1, shiv2, shiv3, shiv4, shiv5)

        val feedForSong1 = BhajanSearch.youtubeFeedRecommendations(
            anchor = shiv1,
            catalog = catalog,
            youtubeResults = emptyList(),
            playedTrackIds = setOf("s1"),
            rotationSeed = 1
        )
        val topPick = feedForSong1.first()

        // Playing topPick next excludes topPick and deprioritizes already-played s1
        val feedForSong2 = BhajanSearch.youtubeFeedRecommendations(
            anchor = topPick,
            catalog = catalog,
            youtubeResults = emptyList(),
            playedTrackIds = setOf("s1", topPick.id),
            rotationSeed = 2
        )

        assertFalse(feedForSong2.any { it.id == topPick.id })
        assertNotEquals("s1", feedForSong2.first().id)
        assertNotEquals(feedForSong1.map { it.id }, feedForSong2.map { it.id })
    }

    @Test fun defaultAartiSongIsReservedForPujaCompletionAndAllOtherFeedSongsAreYoutube() {
        val defaultAarti = BhajanPlayer.Track(
            title = "Jai Ganesh Jai Ganesh Deva",
            url = "https://archive.org/download/JaiGanesh.mp3",
            id = "jai_ganesh_deva",
            deity = "ganesh",
            collection = "ganesh",
            youtubeId = "87Y-OH_mJxM",
            isYoutube = false
        )
        val ytGanesh = BhajanPlayer.Track(
            title = "Sukh Karta Dukh Harta",
            url = "https://www.youtube.com/watch?v=87Y-OH_mJxM",
            id = "yt_sukh_karta",
            deity = "ganesh",
            collection = "ganesh",
            youtubeId = "87Y-OH_mJxM",
            isYoutube = true
        )
        val ytHanuman = BhajanPlayer.Track(
            title = "Shree Hanuman Chalisa",
            url = "https://www.youtube.com/watch?v=AETFvQonfV8",
            id = "yt_hanuman_chalisa",
            deity = "hanuman",
            collection = "hanuman",
            youtubeId = "AETFvQonfV8",
            isYoutube = true
        )

        val feed = BhajanSearch.youtubeFeedRecommendations(
            anchor = defaultAarti,
            catalog = listOf(defaultAarti, ytGanesh, ytHanuman),
            youtubeResults = emptyList(),
            idol = "original"
        )

        assertFalse(feed.any { it.id == "jai_ganesh_deva" })
        assertTrue(feed.all { it.isYoutubeOnly && it.needsStreamResolution })
    }
}




