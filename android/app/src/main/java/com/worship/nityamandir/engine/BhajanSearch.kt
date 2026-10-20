package com.worship.nityamandir.engine

import java.util.Locale

/** Search titles as well as tags: Archive uploads do not share a consistent taxonomy. */
object BhajanSearch {
    private const val devotional = "(title:(bhajan OR bhajans OR aarti OR arti OR arati OR chalisa OR chalisha OR mantra OR stotra OR kirtan OR भजन OR आरती OR चालीसा OR मंत्र) OR subject:(bhajan OR bhajans OR aarti OR chalisa OR mantra OR stotra OR kirtan OR भजन OR आरती))"

    private val hindiAndAliasExpansions = mapOf(
        "हनुमान" to "hanuman bajrang maruti sankat mochan",
        "बजरंग" to "bajrang hanuman",
        "मारुति" to "maruti hanuman",
        "शिव" to "shiva shiv mahadev bhola shankar karpur",
        "महादेव" to "mahadev shiva shiv",
        "भोले" to "bhola bholenath shiva shiv",
        "भोला" to "bhola bholenath shiva shiv",
        "शंकर" to "shankar shiva shiv",
        "तांडव" to "tandav tandava shiva",
        "गणेश" to "ganesh ganpati gajanan vakratunda sukh karta",
        "गणपति" to "ganpati ganesh gajanan morya moriya",
        "गजानन" to "gajanan ganesh ganpati",
        "माता" to "durga mata ambe jagdambe bhawan",
        "दुर्गा" to "durga mata ambe jagdambe",
        "अम्बे" to "ambe jagdambe durga mata",
        "राम" to "ram rama aayenge raghav",
        "कृष्ण" to "krishna shyam shyama bihari achyutam narayan",
        "श्याम" to "shyam shyama krishna bihari vrindavan",
        "नारायण" to "narayan shriman vishnu jagdish",
        "लक्ष्मी" to "lakshmi jagdish mata",
        "आरती" to "aarti arti arati deva sukh karta jagdish",
        "चालीसा" to "chalisa chalisha",
        "मंत्र" to "mantra mahamrityunjaya gayatri athrva",
        "भजन" to "bhajan",
        "chalisha" to "chalisa",
        "arti" to "aarti",
        "arati" to "aarti",
        "raam" to "ram",
        "shivay" to "shivaya shiv",
        "moriya" to "morya",
        "bholenath" to "bhola shiva"
    )

    fun query(input: String, deity: String = ""): String {
        val words = input.take(100).lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
            .trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return "mediatype:audio AND $devotional" + deityFilter(deity)
        val terms = words.joinToString(" AND ") { word ->
            when (word) {
                "chalisha", "chalisa" -> "(\"chalisa\" OR \"chalisha\")"
                "arati", "arti", "aarti" -> "(\"aarti\" OR \"arti\" OR \"arati\")"
                else -> "\"$word\""
            }
        }
        return "mediatype:audio AND $devotional AND (title:($terms) OR subject:($terms))" + deityFilter(deity)
    }

    private fun deityFilter(deity: String): String {
        val terms = deity.replace(Regex("[^\\p{L}\\p{M}\\s]"), " ").trim()
            .split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" OR ") { "\"$it\"" }
        return if (terms.isEmpty()) "" else " AND (title:($terms) OR subject:($terms))"
    }

    /** Matches a user search query (in Hindi or English) against a catalog track. */
    fun matchesTrack(
        rawQuery: String,
        title: String,
        artist: String,
        deity: String,
        collection: String
    ): Boolean {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return true
        val haystack = buildString {
            append(title.lowercase(Locale.ROOT)).append(' ')
            append(artist.lowercase(Locale.ROOT)).append(' ')
            append(deity.lowercase(Locale.ROOT)).append(' ')
            append(collection.lowercase(Locale.ROOT)).append(' ')
            when (deity.lowercase(Locale.ROOT)) {
                "shiva" -> append("shiv mahadev bhola bholenath shankar शिव महादेव भोले शंकर ")
                "hanuman" -> append("bajrang maruti sankat mochan हनुमान बजरंग मारुति ")
                "durga" -> append("mata rani ambe jagdambe sherawali माता दुर्गा अम्बे ")
                "ganesh" -> append("ganpati gajanan bappa morya moriya vinayak गणेश गणपति गजानन ")
                "rama", "ram" -> append("ram rama raghav siya ayodhya राम सिया ")
                "krishna" -> append("shyam shyama kanha bihari radhe narayan कृष्ण श्याम बांके बिहारी ")
                "lakshmi" -> append("luxmi mahalakshmi jagdish लक्ष्मी ")
            }
        }
        if (haystack.contains(q)) return true
        val tokens = q.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it != "भजन" && it != "bhajan" && it != "जी" && it != "ji" }
        if (tokens.isEmpty()) return true
        return tokens.all { token ->
            if (haystack.contains(token)) true
            else {
                val expanded = hindiAndAliasExpansions[token]
                expanded != null && expanded.split(' ').any { alias -> haystack.contains(alias) }
            }
        }
    }

    private val rendererSplitPattern = Regex(
        """"(?:videoRenderer|compactVideoRenderer|videoWithContextRenderer)"\s*:\s*\{"""
    )
    private val videoIdPattern = Regex(""""videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""")
    private val titleRunPattern = Regex(""""title"\s*:\s*\{\s*(?:"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([^"]+)"|"simpleText"\s*:\s*"([^"]+)"|"accessibility"\s*:\s*\{\s*"accessibilityData"\s*:\s*\{\s*"label"\s*:\s*"([^"]+)")""")
    private val ownerRunPattern = Regex("""(?:"ownerText"|"longBylineText"|"shortBylineText")\s*:\s*\{\s*(?:"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([^"]+)"|"simpleText"\s*:\s*"([^"]+)")""")
    private val lengthPattern = Regex(""""lengthText"\s*:\s*\{[^}]*(?:"simpleText"\s*:\s*"([0-9:]+)"|"runs"\s*:\s*\[\s*\{\s*"text"\s*:\s*"([0-9:]+)")""")

    /** Extracts playable YouTube bhajan tracks from YouTube Innertube or search results payloads. */
    fun parseYoutubeSearchResults(payload: String, defaultDeity: String = "mixed", limit: Int = 18): List<BhajanPlayer.Track> {
        val results = mutableListOf<BhajanPlayer.Track>()
        val seen = mutableSetOf<String>()
        val blocks = rendererSplitPattern.split(payload).drop(1)
        for (rawBlock in blocks) {
            val block = rawBlock.take(4200)
            val videoId = videoIdPattern.find(block)?.groupValues?.getOrNull(1).orEmpty()
            if (videoId.length != 11 || !seen.add(videoId)) continue
            val titleMatch = titleRunPattern.find(block)
            val rawTitle = listOfNotNull(
                titleMatch?.groupValues?.getOrNull(1),
                titleMatch?.groupValues?.getOrNull(2),
                titleMatch?.groupValues?.getOrNull(3)
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val cleanTitle = unescapeJsonText(rawTitle).trim()
            if (cleanTitle.isEmpty()) continue
            val ownerMatch = ownerRunPattern.find(block)
            val rawArtist = listOfNotNull(
                ownerMatch?.groupValues?.getOrNull(1),
                ownerMatch?.groupValues?.getOrNull(2)
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val artist = unescapeJsonText(rawArtist).ifBlank { "YouTube Bhakti" }
            val lenMatch = lengthPattern.find(block)
            val durationStr = listOfNotNull(
                lenMatch?.groupValues?.getOrNull(1),
                lenMatch?.groupValues?.getOrNull(2)
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val seconds = parseDurationSeconds(durationStr)
            if (seconds in 1..44) continue // Skip YouTube Shorts
            val inferredDeity = inferDeity("$cleanTitle $artist", defaultDeity)
            results.add(
                BhajanPlayer.Track(
                    title = cleanTitle,
                    url = "https://www.youtube.com/watch?v=$videoId",
                    id = "yt_$videoId",
                    order = results.size + 1,
                    collection = inferredDeity,
                    deity = inferredDeity,
                    thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    artist = if (artist.contains("YouTube", ignoreCase = true)) artist else "$artist • YouTube",
                    durationSeconds = seconds,
                    youtubeId = videoId,
                    isYoutube = true
                )
            )
            if (results.size >= limit) break
        }
        return results
    }

    /** Parses YouTube autocomplete suggestion array payload `["query", ["suggestion 1", ...]]`. */
    fun parseYoutubeSuggestResults(payload: String, limit: Int = 8): List<String> {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return emptyList()
        return runCatching {
            val jsonStart = trimmed.indexOf('[')
            val jsonEnd = trimmed.lastIndexOf(']')
            if (jsonStart < 0 || jsonEnd <= jsonStart) return emptyList()
            val root = org.json.JSONArray(trimmed.substring(jsonStart, jsonEnd + 1))
            val suggestionsArray = root.optJSONArray(1) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until suggestionsArray.length()) {
                val entry = suggestionsArray.opt(i)
                val text = when (entry) {
                    is String -> entry
                    is org.json.JSONArray -> entry.optString(0)
                    else -> ""
                }.trim()
                if (text.isNotBlank() && list.none { it.equals(text, ignoreCase = true) }) {
                    list.add(text)
                    if (list.size >= limit) break
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    private val nextWordGraphEn = mapOf(
        "hanuman" to listOf("chalisa", "ashtak", "aarti", "bhajan", "amritwani", "mantra", "baan"),
        "bajrang" to listOf("baan", "bali bhajan", "chalisa", "aarti"),
        "sankat" to listOf("mochan", "mochan hanuman ashtak", "harana"),
        "shiv" to listOf("tandav", "chalisa", "aarti", "bhajan", "dhun", "stotram", "mantra"),
        "shiva" to listOf("tandava stotram", "chalisa", "bhajan", "mantra", "dhun", "aarti"),
        "mahadev" to listOf("bhajan", "aarti", "tandav", "har har mahadev", "mantra"),
        "bhola" to listOf("hai bhandari", "bholenath bhajan", "nath aarti"),
        "ganesh" to listOf("aarti", "chalisa", "vandana", "mantra", "bhajan", "deva"),
        "ganpati" to listOf("bappa morya", "aarti", "vandana", "bhajan", "atharvashirsha"),
        "sukh" to listOf("karta dukh harta", "karta aarti"),
        "ram" to listOf("aayenge", "chalisa", "bhajan", "aarti", "dhun", "stuti", "siya ram"),
        "raam" to listOf("aayenge", "bhajan", "chalisa", "stuti", "dhun"),
        "shri" to listOf("hanuman chalisa", "ramachandra kripalu", "krishna govind", "ganesh deva"),
        "shree" to listOf("hanuman chalisa", "ganesh aarti", "ram bhajan", "durga chalisa"),
        "krishna" to listOf("bhajan", "aarti", "chalisa", "flute", "govind hare murari", "vandana"),
        "shyam" to listOf("bhajan", "aan baso vrindavan", "aarti", "teri bansi"),
        "banke" to listOf("bihari laal", "bihari aarti", "bihari bhajan"),
        "durga" to listOf("chalisa", "aarti", "bhajan", "stuti", "mantra", "kavach"),
        "mata" to listOf("bhajan", "aarti", "ke bhajan", "rani chalisa", "jagdambe"),
        "ambe" to listOf("tu hai jagdambe kali", "maa aarti", "bhawani"),
        "lakshmi" to listOf("aarti", "chalisa", "mantra", "bhajan", "mata aarti"),
        "om" to listOf("namah shivay", "jai jagdish hare", "gan ganpataye namo namah", "mantra", "chanting"),
        "jai" to listOf("ganesh deva", "hanuman gyan gun sagar", "shri ram", "mata di", "jagdish hare"),
        "har" to listOf("har mahadev", "har shiv shankar", "har gange"),
        "aarti" to listOf("ganesh", "hanuman", "shiv", "durga", "krishna", "kunj bihari ki", "sangrah"),
        "chalisa" to listOf("hanuman", "shiv", "durga", "ganesh", "ram", "lakshmi"),
        "bhajan" to listOf("hanuman", "krishna", "shiv", "ram", "mata", "morning", "non stop"),
        "mantra" to listOf("mahamrityunjaya", "gayatri", "ganesh", "shiv", "om namah shivay")
    )

    private val nextWordGraphHi = mapOf(
        "हनुमान" to listOf("चालीसा", "अष्टक", "आरती", "भजन", "अमृतवाणी", "बजरंग बाण"),
        "शिव" to listOf("तांडव", "चालीसा", "आरती", "भजन", "धुन", "महामृत्युंजय मंत्र"),
        "महादेव" to listOf("भजन", "आरती", "तांडव", "हर हर महादेव"),
        "गणेश" to listOf("आरती", "चालीसा", "वंदना", "मंत्र", "भजन"),
        "गणपति" to listOf("बप्पा मोरया", "आरती", "वंदना", "भजन"),
        "राम" to listOf("आएंगे", "चालीसा", "भजन", "आरती", "धुन", "स्तुति"),
        "कृष्ण" to listOf("भजन", "आरती", "चालीसा", "बांके बिहारी", "धुन"),
        "श्याम" to listOf("भजन", "आन बसो वृंदावन में", "आरती"),
        "दुर्गा" to listOf("चालीसा", "आरती", "भजन", "स्तुति", "मंत्र"),
        "माता" to listOf("के भजन", "आरती", "चालीसा", "रानी भजन"),
        "लक्ष्मी" to listOf("आरती", "चालीसा", "मंत्र", "भजन"),
        "जय" to listOf("गणेश देवा", "हनुमान", "श्री राम", "माता दी", "जगदीश हरे"),
        "ॐ" to listOf("नमः शिवाय", "जय जगदीश हरे", "गण गणपतये नमः", "मंत्र"),
        "श्री" to listOf("हनुमान चालीसा", "रामचंद्र कृपालु", "कृष्ण गोविंद", "गणेश देवा"),
        "आरती" to listOf("गणेश जी की", "हनुमान जी की", "शिव जी की", "कुंज बिहारी की", "माता की"),
        "चालीसा" to listOf("हनुमान", "शिव", "दुर्गा", "गणेश"),
        "भजन" to listOf("हनुमान", "कृष्ण", "शिव", "राम", "माता")
    )

    private val starterVocabularyEn = listOf(
        "hanuman", "chalisa", "shiv", "tandav", "ganesh", "aarti", "ram", "aayenge",
        "krishna", "bhajan", "durga", "mantra", "mahamrityunjaya", "sankat", "mochan",
        "sukh", "karta", "ambe", "jagdambe", "shriman", "narayan", "banke", "bihari"
    )

    /**
     * Returns YouTube-style recommended next words (or starter phrases when query is empty)
     * based on the user's current search input, active track, and catalog titles.
     */
    fun recommendedNextWords(
        query: String,
        hindi: Boolean = false,
        anchor: BhajanPlayer.Track? = null,
        catalog: List<BhajanPlayer.Track> = emptyList(),
        youtubeResults: List<BhajanPlayer.Track> = emptyList(),
        idol: String = "original"
    ): List<String> {
        val clean = query.trim()
        val targetDeity = anchor?.deity?.takeIf { it.isNotBlank() } ?: when (idol) {
            "original", "ganesh_hanuman" -> "ganesh"
            "radha_krishna" -> "krishna"
            "ram_darbar" -> "rama"
            else -> idol
        }

        if (clean.isEmpty()) {
            return if (hindi) {
                val deityFirst = when (targetDeity) {
                    "hanuman" -> listOf("हनुमान चालीसा", "संकट मोचन", "बजरंग बाण")
                    "shiva" -> listOf("शिव तांडव", "ॐ नमः शिवाय", "महामृत्युंजय मंत्र")
                    "durga" -> listOf("दुर्गा चालीसा", "अम्बे तू है जगदम्बे", "माता भजन")
                    "rama" -> listOf("राम आएंगे", "श्री रामचंद्र कृपालु", "राम भजन")
                    "krishna" -> listOf("बांके बिहारी", "श्याम भजन", "अच्युतम केशवम")
                    "lakshmi" -> listOf("ॐ जय जगदीश हरे", "श्रीमन नारायण", "लक्ष्मी आरती")
                    else -> listOf("जय गणेश देवा", "सुखकर्ता दुखहर्ता", "गणपति बप्पा मोरया")
                }
                (deityFirst + listOf("हनुमान चालीसा", "शिव तांडव", "गणेश आरती", "राम आएंगे", "कृष्ण भजन", "दुर्गा चालीसा")).distinct().take(8)
            } else {
                val deityFirst = when (targetDeity) {
                    "hanuman" -> listOf("Hanuman Chalisa", "Sankat Mochan", "Bajrang Baan")
                    "shiva" -> listOf("Shiv Tandav", "Om Namah Shivay", "Mahamrityunjaya")
                    "durga" -> listOf("Durga Chalisa", "Ambe Tu Hai", "Mata Bhajan")
                    "rama" -> listOf("Raam Aayenge", "Shri Ram Stuti", "Ram Bhajan")
                    "krishna" -> listOf("Banke Bihari", "Krishna Bhajan", "Shyama Aan Baso")
                    "lakshmi" -> listOf("Om Jai Jagdish", "Shriman Narayan", "Lakshmi Aarti")
                    else -> listOf("Jai Ganesh Deva", "Sukh Karta", "Ganpati Bappa Morya")
                }
                (deityFirst + listOf("Hanuman Chalisa", "Shiv Tandav", "Ganesh Aarti", "Raam Aayenge", "Krishna Bhajan", "Durga Chalisa")).distinct().take(8)
            }
        }

        val tokens = clean.lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val lastToken = tokens.lastOrNull().orEmpty()
        val existingSet = tokens.toSet()
        val suggestions = mutableListOf<String>()

        // 1. Direct next-word lookup from Hindi or English devotional graph
        nextWordGraphHi[lastToken]?.forEach { next ->
            if (next.lowercase(Locale.ROOT) !in existingSet) suggestions.add(next)
        }
        nextWordGraphEn[lastToken]?.forEach { next ->
            if (next.lowercase(Locale.ROOT) !in existingSet) suggestions.add(next)
        }

        // 2. If user is mid-word (e.g. "hanu", "chal", "tan"), suggest completing the word + its next word
        if (suggestions.isEmpty() && lastToken.length >= 2) {
            for ((key, nextList) in nextWordGraphEn) {
                if (key.startsWith(lastToken) && key != lastToken) {
                    suggestions.add(key)
                    nextList.firstOrNull()?.let { suggestions.add("$key $it") }
                }
            }
            for (vocab in starterVocabularyEn) {
                if (vocab.startsWith(lastToken) && vocab != lastToken && vocab !in suggestions) {
                    suggestions.add(vocab)
                }
            }
        }

        // 3. Mine following words from catalog & YouTube track titles
        val allTracks = youtubeResults + catalog
        for (track in allTracks) {
            val titleWords = track.title
                .replace(Regex("[()\\[\\]|•-]"), " ")
                .trim()
                .split(Regex("\\s+"))
                .filter { it.length >= 2 }
            for (i in 0 until titleWords.size - 1) {
                val w = titleWords[i].lowercase(Locale.ROOT)
                val nextW = titleWords[i + 1]
                val nextLower = nextW.lowercase(Locale.ROOT)
                if ((w == lastToken || (lastToken.length >= 3 && w.startsWith(lastToken))) &&
                    nextLower !in existingSet &&
                    suggestions.none { it.equals(nextW, ignoreCase = true) }
                ) {
                    suggestions.add(nextLower)
                }
            }
        }

        // 4. Always provide通用 devotional next-word completions if still sparse
        val fallbacks = if (hindi || clean.any { it.code in 0x0900..0x097F }) {
            listOf("भजन", "आरती", "चालीसा", "मंत्र", "धुन", "स्तुति")
        } else {
            listOf("bhajan", "aarti", "chalisa", "mantra", "stotram", "dhun", "live")
        }
        for (fb in fallbacks) {
            if (fb.lowercase(Locale.ROOT) !in existingSet && suggestions.none { it.equals(fb, ignoreCase = true) }) {
                suggestions.add(fb)
            }
        }

        return suggestions.distinct().take(8)
    }

    /**
     * Builds full YouTube autocomplete search rows combining live YouTube suggestions,
     * next-word phrase completions, and matching catalog/YouTube titles.
     */
    fun autocompletePredictions(
        query: String,
        hindi: Boolean = false,
        catalog: List<BhajanPlayer.Track> = emptyList(),
        youtubeResults: List<BhajanPlayer.Track> = emptyList(),
        remoteSuggestions: List<String> = emptyList(),
        anchor: BhajanPlayer.Track? = null,
        idol: String = "original"
    ): List<String> {
        val clean = query.trim()
        if (clean.isEmpty()) {
            return recommendedNextWords("", hindi, anchor, catalog, youtubeResults, idol)
        }
        val results = mutableListOf<String>()
        // 1. Next-word phrase completions (e.g. "hanuman" -> "hanuman chalisa", "hanuman ashtak")
        val nextWords = recommendedNextWords(clean, hindi, anchor, catalog, youtubeResults, idol)
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lastToken = tokens.lastOrNull()?.lowercase(Locale.ROOT).orEmpty()
        for (nw in nextWords.take(4)) {
            val phrase = if (nw.lowercase(Locale.ROOT).startsWith(lastToken) && nw.lowercase(Locale.ROOT) != lastToken) {
                (tokens.dropLast(1) + nw).joinToString(" ")
            } else {
                "$clean $nw"
            }
            if (results.none { it.equals(phrase, ignoreCase = true) }) {
                results.add(phrase)
            }
        }
        // 2. Live YouTube autocomplete suggestions
        for (remote in remoteSuggestions) {
            val cleanedRemote = remote.trim()
            if (cleanedRemote.isNotBlank() && results.none { it.equals(cleanedRemote, ignoreCase = true) }) {
                results.add(cleanedRemote)
            }
        }
        // 3. Matching track titles from YouTube & catalog
        for (track in (youtubeResults + catalog)) {
            if (matchesTrack(clean, track.title, track.artist, track.deity, track.collection)) {
                val shortTitle = track.title.substringBefore("(").substringBefore("|").trim()
                if (shortTitle.isNotBlank() && results.none { it.equals(shortTitle, ignoreCase = true) }) {
                    results.add(shortTitle)
                }
            }
            if (results.size >= 8) break
        }
        return results.take(6)
    }

    private val audioStreamUrlPattern = Regex(
        """"mimeType"\s*:\s*"audio/(?:mp4|webm)[^"]*"[^}]*?"url"\s*:\s*"(https://[^"]+)""""
    )
    private val reverseAudioStreamUrlPattern = Regex(
        """"url"\s*:\s*"(https://[^"]+)"[^}]*?"mimeType"\s*:\s*"audio/(?:mp4|webm)[^"]*""""
    )
    private val fallbackVideoStreamUrlPattern = Regex(
        """"url"\s*:\s*"(https://[^"]+)"[^}]*?"mimeType"\s*:\s*"video/mp4[^"]*""""
    )

    /** Extracts a direct playable audio/media stream URL from a YouTube Innertube player response. */
    fun parseYoutubePlayerStreamUrl(payload: String): String? {
        runCatching {
            val streamingData = org.json.JSONObject(payload).optJSONObject("streamingData")
            if (streamingData != null) {
                val adaptive = streamingData.optJSONArray("adaptiveFormats")
                var bestAudioMp4: String? = null
                var bestAudioAny: String? = null
                if (adaptive != null) {
                    for (i in 0 until adaptive.length()) {
                        val fmt = adaptive.optJSONObject(i) ?: continue
                        val url = fmt.optString("url").trim()
                        if (!url.startsWith("https://")) continue
                        val mime = fmt.optString("mimeType").lowercase(Locale.ROOT)
                        val itag = fmt.optInt("itag", 0)
                        if (itag == 140) return url
                        if (mime.startsWith("audio/mp4") && bestAudioMp4 == null) {
                            bestAudioMp4 = url
                        } else if (mime.startsWith("audio/") && bestAudioAny == null) {
                            bestAudioAny = url
                        }
                    }
                }
                if (bestAudioMp4 != null) return bestAudioMp4
                if (bestAudioAny != null) return bestAudioAny

                val formats = streamingData.optJSONArray("formats")
                if (formats != null) {
                    for (i in 0 until formats.length()) {
                        val fmt = formats.optJSONObject(i) ?: continue
                        val url = fmt.optString("url").trim()
                        if (url.startsWith("https://")) return url
                    }
                }
            }
        }
        val raw = audioStreamUrlPattern.find(payload)?.groupValues?.getOrNull(1)
            ?: reverseAudioStreamUrlPattern.find(payload)?.groupValues?.getOrNull(1)
            ?: fallbackVideoStreamUrlPattern.find(payload)?.groupValues?.getOrNull(1)
            ?: return null
        return unescapeJsonText(raw).takeIf { it.startsWith("https://") }
    }

    private val devotionalKeywords = setOf(
        "aarti", "arti", "chalisa", "stotram", "stotra", "mantra", "ashtak", "dhun",
        "tandav", "vandana", "baan", "amritwani", "bhajan", "kirtan", "stuti", "prarthana"
    )

    private val hindiToLatinSongTokens = mapOf(
        "श्री" to "",
        "हनुमान" to "hanuman",
        "चालीसा" to "chalisa",
        "संकट" to "sankat",
        "मोचन" to "mochan",
        "अष्टक" to "ashtak",
        "बजरंग" to "bajrang",
        "बाण" to "baan",
        "शिव" to "shiv",
        "तांडव" to "tandav",
        "स्तोत्रम" to "stotram",
        "स्तोत्र" to "stotram",
        "महादेव" to "mahadev",
        "भोला" to "bhola",
        "भोले" to "bhola",
        "भंडारी" to "bhandari",
        "महामृत्युंजय" to "mahamrityunjaya",
        "मंत्र" to "mantra",
        "गणेश" to "ganesh",
        "गणपति" to "ganpati",
        "बप्पा" to "bappa",
        "मोरया" to "morya",
        "गजानन" to "gajanan",
        "सुखकर्ता" to "sukh karta",
        "दुखहर्ता" to "dukh harta",
        "दुर्गा" to "durga",
        "अम्बे" to "ambe",
        "जगदम्बे" to "jagdambe",
        "काली" to "kali",
        "माता" to "mata",
        "राम" to "ram",
        "आएंगे" to "aayenge",
        "कृष्ण" to "krishna",
        "बांके" to "banke",
        "बिहारी" to "bihari",
        "श्याम" to "shyam",
        "नारायण" to "narayan",
        "लक्ष्मी" to "lakshmi",
        "आरती" to "aarti",
        "भजन" to "bhajan"
    )

    /**
     * Normalizes a bhajan title so duplicate entries across `music_catalog.json`,
     * `youtubeFeaturedTracks`, and live YouTube search results are recognized as the same song.
     */
    fun normalizeSongTitle(raw: String): String {
        val core = raw
            .substringBefore("(")
            .substringBefore("[")
            .substringBefore("|")
            .substringBefore(" - ")
            .substringBefore(" __ ")
        return normalizeSegment(core).ifBlank { normalizeSegment(raw) }
    }

    private fun normalizeSegment(segment: String): String {
        var text = segment.lowercase(Locale.ROOT)
        for ((hi, en) in hindiToLatinSongTokens) {
            if (text.contains(hi)) {
                text = text.replace(hi, " $en ")
            }
        }
        val words = text
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .mapNotNull { token ->
                when (token) {
                    "", "shree", "shri", "sri", "o", "oh", "original", "official",
                    "video", "audio", "full", "hd", "4k", "lyrics", "lyrical", "song" -> null
                    "raam" -> "ram"
                    "moriya" -> "morya"
                    "chalisha" -> "chalisa"
                    "arti", "arati" -> "aarti"
                    "ambey" -> "ambe"
                    "jagdambey" -> "jagdambe"
                    "shanker" -> "shankar"
                    "tandava" -> "tandav"
                    "shiva" -> "shiv"
                    "lala" -> "laal"
                    "ban" -> "baan"
                    "bechne" -> "bechane"
                    else -> token
                }
            }
        return words.joinToString(" ")
    }

    private fun normalizedTitleSegments(raw: String): List<String> {
        val primary = normalizeSongTitle(raw)
        val parts = raw.split('|', '(', ')', '[', ']')
            .map { normalizeSegment(it) }
            .filter { it.length >= 8 }
        return (listOf(primary) + parts).filter { it.isNotBlank() }.distinct()
    }

    /**
     * Returns true if [a] and [b] represent the same song (by id, base audio URL, or normalized title).
     */
    fun isSameSong(a: BhajanPlayer.Track?, b: BhajanPlayer.Track?): Boolean {
        if (a == null || b == null) return false
        if (a.id.equals(b.id, ignoreCase = true)) return true

        val urlA = a.url.substringBefore('#').trim()
        val urlB = b.url.substringBefore('#').trim()
        if (urlA.isNotBlank() && urlA.equals(urlB, ignoreCase = true)) return true

        val normA = normalizeSongTitle(a.title)
        val normB = normalizeSongTitle(b.title)
        if (normA.isNotBlank() && normA == normB) return true

        // If both share the same non-blank YouTube video ID and are not two distinct catalog songs
        if (a.youtubeId.isNotBlank() && a.youtubeId.equals(b.youtubeId, ignoreCase = true)) {
            val bothAreDistinctCatalogTitles = normA.isNotBlank() && normB.isNotBlank() &&
                !normA.contains(normB) && !normB.contains(normA) &&
                !normA.startsWith(normB.take(10)) && !normB.startsWith(normA.take(10)) &&
                (normA.contains("jai ganesh") != normB.contains("jai ganesh"))
            if (!bothAreDistinctCatalogTitles) return true
        }

        val segmentsA = normalizedTitleSegments(a.title)
        val segmentsB = normalizedTitleSegments(b.title)
        for (segA in segmentsA) {
            for (segB in segmentsB) {
                if (segA == segB) return true
                if (segA.length >= 10 && segB.length >= 10 && (segA.contains(segB) || segB.contains(segA))) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Removes any track matching [exclude] (same song) and deduplicates twins within [tracks].
     */
    fun deduplicateTracks(
        tracks: List<BhajanPlayer.Track>,
        exclude: BhajanPlayer.Track? = null
    ): List<BhajanPlayer.Track> {
        val result = mutableListOf<BhajanPlayer.Track>()
        for (track in tracks) {
            if (exclude != null && isSameSong(track, exclude)) continue
            if (result.any { isSameSong(it, track) }) continue
            result.add(track)
        }
        return result
    }

    /**
     * Ranks and interleaves Pavitra Mandir (Temple icon) and YouTube (YouTube icon) recommendations
     * relevant to the currently playing [anchor] track or active [searchQuery], just like YouTube.
     */
    fun relevantRecommendations(
        anchor: BhajanPlayer.Track?,
        catalog: List<BhajanPlayer.Track>,
        youtubeResults: List<BhajanPlayer.Track>,
        searchQuery: String = "",
        idol: String = "original"
    ): List<BhajanPlayer.Track> {
        val q = searchQuery.trim()
        val targetDeity = anchor?.deity?.takeIf { it.isNotBlank() } ?: when (idol) {
            "original", "ganesh_hanuman" -> "ganesh"
            "radha_krishna" -> "krishna"
            "ram_darbar" -> "rama"
            else -> idol
        }

        val mandirCandidates = deduplicateTracks(
            tracks = catalog
                .filter { !it.isYoutubeOnly }
                .filter {
                    if (q.isEmpty()) !isSameSong(it, anchor)
                    else matchesTrack(q, it.title, it.artist, it.deity, it.collection)
                }
                .sortedByDescending { candidate ->
                    relevanceScore(candidate, anchor, targetDeity, q)
                },
            exclude = if (q.isEmpty()) anchor else null
        )

        val catalogYoutubeCandidates = catalog
            .filter { it.isYoutubeOnly }
            .filter {
                if (q.isEmpty()) !isSameSong(it, anchor)
                else matchesTrack(q, it.title, it.artist, it.deity, it.collection)
            }

        val liveYoutubeCandidates = youtubeResults
            .filter { !isSameSong(it, anchor) }

        val allYoutubeCandidates = deduplicateTracks(
            tracks = (liveYoutubeCandidates + catalogYoutubeCandidates)
                .sortedByDescending { candidate ->
                    relevanceScore(candidate, anchor, targetDeity, q) +
                        if (candidate in liveYoutubeCandidates) 30 else 0
                },
            exclude = if (q.isEmpty()) anchor else null
        )

        // Interleave Pavitra Mandir (Temple icon) and YouTube (YouTube icon) relevant tracks
        val combined = mutableListOf<BhajanPlayer.Track>()
        val maxLen = maxOf(mandirCandidates.size, allYoutubeCandidates.size)
        for (i in 0 until maxLen) {
            if (i < mandirCandidates.size) combined.add(mandirCandidates[i])
            if (i < allYoutubeCandidates.size) combined.add(allYoutubeCandidates[i])
        }
        return deduplicateTracks(combined, exclude = if (q.isEmpty()) anchor else null)
    }

    /**
     * Loose token match used as a fallback when a multi-word search query contains extra words
     * not present in the local catalog title, ensuring the screen still refreshes immediately
     * with relevant tracks while live YouTube results load.
     */
    fun matchesAnyToken(
        rawQuery: String,
        title: String,
        artist: String,
        deity: String,
        collection: String
    ): Int {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return 0
        val haystack = buildString {
            append(title.lowercase(Locale.ROOT)).append(' ')
            append(artist.lowercase(Locale.ROOT)).append(' ')
            append(deity.lowercase(Locale.ROOT)).append(' ')
            append(collection.lowercase(Locale.ROOT)).append(' ')
            when (deity.lowercase(Locale.ROOT)) {
                "shiva" -> append("shiv mahadev bhola bholenath shankar शिव महादेव भोले शंकर ")
                "hanuman" -> append("bajrang maruti sankat mochan हनुमान बजरंग मारुति ")
                "durga" -> append("mata rani ambe jagdambe sherawali माता दुर्गा अम्बे ")
                "ganesh" -> append("ganpati gajanan bappa morya moriya vinayak गणेश गणपति गजानन ")
                "rama", "ram" -> append("ram rama raghav siya ayodhya राम सिया ")
                "krishna" -> append("shyam shyama kanha bihari radhe narayan कृष्ण श्याम बांके बिहारी ")
                "lakshmi" -> append("luxmi mahalakshmi jagdish लक्ष्मी ")
            }
        }
        val tokens = q.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it != "भजन" && it != "bhajan" && it != "जी" && it != "ji" }
        if (tokens.isEmpty()) return 1
        var matchedCount = 0
        for (token in tokens) {
            val hit = haystack.contains(token) ||
                (hindiAndAliasExpansions[token]?.split(' ')?.any { alias -> haystack.contains(alias) } == true)
            if (hit) matchedCount++
        }
        return matchedCount
    }

    private fun mixDynamicSeed(anchorHash: Int, candidateHash: Int, rotationSeed: Int): Int {
        var h = anchorHash xor (candidateHash * 0x9E3779B9.toInt()) xor (rotationSeed * 0x85EBCA6B.toInt())
        h = h xor (h ushr 16)
        h *= 0x7FEB352D
        h = h xor (h ushr 15)
        return h and 0x7FFFFFFF
    }

    /**
     * Returns a YouTube-first dynamic recommendation feed for the YouTube player UI.
     * - Strictly excludes the currently playing [anchor] song (and any twin/re-upload of it).
     * - Dynamically reorders recommendations whenever [anchor], [playedTrackIds], or [rotationSeed] changes.
     * - When [searchQuery] is non-empty, filters and ranks tracks strictly by search relevance.
     */
    fun youtubeFeedRecommendations(
        anchor: BhajanPlayer.Track?,
        catalog: List<BhajanPlayer.Track>,
        youtubeResults: List<BhajanPlayer.Track>,
        searchQuery: String = "",
        categoryFilter: String = "all",
        idol: String = "original",
        playedTrackIds: Set<String> = emptySet(),
        rotationSeed: Int = 0
    ): List<BhajanPlayer.Track> {
        val q = searchQuery.trim()
        val youtubeCatalog = catalog.filter { it.id != "jai_ganesh_deva" }
        if (q.isNotEmpty()) {
            val inferredSearchDeity = inferDeity(q, "")
            val allCandidates = deduplicateTracks(
                tracks = (youtubeResults + youtubeCatalog).map { track ->
                    if (!track.isYoutube && track.youtubeId.isNotBlank()) track.copy(isYoutube = true) else track
                },
                exclude = null
            )

            val strictMatches = allCandidates.filter { track ->
                youtubeResults.any { it.id == track.id } ||
                    matchesTrack(q, track.title, track.artist, track.deity, track.collection)
            }

            val searchPool = if (strictMatches.size >= 3) {
                strictMatches
            } else {
                val partialMatches = allCandidates.filter { track ->
                    matchesAnyToken(q, track.title, track.artist, track.deity, track.collection) > 0
                }
                deduplicateTracks(strictMatches + partialMatches)
            }

            val qLower = q.lowercase(Locale.ROOT)
            return searchPool.sortedByDescending { track ->
                val isLiveSearchResult = youtubeResults.any { it.id == track.id }
                val isStrict = matchesTrack(q, track.title, track.artist, track.deity, track.collection)
                val tokenHits = matchesAnyToken(q, track.title, track.artist, track.deity, track.collection)
                val titleLower = track.title.lowercase(Locale.ROOT)
                val exactPrefix = titleLower.startsWith(qLower)
                val exactContains = titleLower.contains(qLower)
                val deityMatch = inferredSearchDeity.isNotBlank() && track.deity.equals(inferredSearchDeity, ignoreCase = true)

                (if (isLiveSearchResult) 400 else 0) +
                    (if (isStrict) 250 else 0) +
                    (tokenHits * 60) +
                    (if (exactPrefix) 120 else if (exactContains) 80 else 0) +
                    (if (deityMatch) 90 else 0)
            }.distinctBy { it.id }
        }

        val targetDeity = anchor?.deity?.takeIf { it.isNotBlank() } ?: when (idol) {
            "original", "ganesh_hanuman" -> "ganesh"
            "radha_krishna" -> "krishna"
            "ram_darbar" -> "rama"
            else -> idol
        }

        // Prefer YouTube-flagged entries first when deduplicating twins, and promote all tracks with a YouTube ID
        // so the entire catalog participates equally in dynamic recommendations instead of static top-12 locking.
        val rawPool = (
            youtubeResults +
                youtubeCatalog.filter { it.isYoutubeOnly } +
                youtubeCatalog.filter { !it.isYoutubeOnly }
            ).map { track ->
            if (!track.isYoutube && track.youtubeId.isNotBlank()) track.copy(isYoutube = true) else track
        }

        val uniqueCandidates = deduplicateTracks(rawPool, exclude = anchor)
        val anchorKey = normalizeSongTitle(anchor?.title.orEmpty()).ifBlank { anchor?.id ?: idol }
        val anchorHash = anchorKey.hashCode()

        val dynamicallyRanked = uniqueCandidates.sortedByDescending { track ->
            val isLiveYt = youtubeResults.any { isSameSong(it, track) }
            val isYtReady = track.isYoutubeOnly || track.youtubeId.isNotBlank()
            val wasRecentlyPlayed = track.id in playedTrackIds
            val candDeity = track.deity.lowercase(Locale.ROOT)
            val normTarget = targetDeity.lowercase(Locale.ROOT)
            val sameDeity = candDeity == normTarget
            val relatedDeity = !sameDeity && areRelatedDeities(candDeity, normTarget)

            // Deterministic per-anchor + per-play dynamic jitter (0..84) that reshuffles recommendations
            // every time the user plays a different song or presses Next.
            val dynamicJitter = mixDynamicSeed(
                anchorHash = anchorHash,
                candidateHash = normalizeSongTitle(track.title).hashCode(),
                rotationSeed = rotationSeed
            ) % 85

            val artistBonus = if (anchor != null) {
                val anchorArtist = anchor.artist.substringBefore("•").trim().lowercase(Locale.ROOT)
                if (anchorArtist.length >= 4 && track.artist.lowercase(Locale.ROOT).contains(anchorArtist)) 22 else 0
            } else 0

            (if (isLiveYt) 160 else 0) +
                (if (isYtReady) 40 else 0) +
                (if (sameDeity) 95 else if (relatedDeity) 55 else 20) +
                artistBonus +
                dynamicJitter -
                (if (wasRecentlyPlayed) 180 else 0)
        }.distinctBy { it.id }

        val filterLower = categoryFilter.lowercase(Locale.ROOT)
        if (filterLower == "all" || filterLower == "up_next") return dynamicallyRanked
        return dynamicallyRanked.filter { track ->
            when (filterLower) {
                "aarti" -> track.title.contains("aarti", ignoreCase = true) || track.title.contains("आरती")
                "chalisa" -> track.title.contains("chalisa", ignoreCase = true) || track.title.contains("चालीसा")
                "mantra" -> track.title.contains("mantra", ignoreCase = true) || track.title.contains("stotram", ignoreCase = true) || track.title.contains("मंत्र")
                else -> track.deity.equals(filterLower, ignoreCase = true) || track.collection.equals(filterLower, ignoreCase = true)
            }
        }.ifEmpty { dynamicallyRanked }
    }

    private fun relevanceScore(
        candidate: BhajanPlayer.Track,
        anchor: BhajanPlayer.Track?,
        targetDeity: String,
        query: String
    ): Int {
        var score = 0
        val candDeity = candidate.deity.lowercase(Locale.ROOT)
        val normTarget = targetDeity.lowercase(Locale.ROOT)

        if (candDeity == normTarget) {
            score += 100
        } else if (areRelatedDeities(candDeity, normTarget)) {
            score += 55
        }

        if (anchor != null) {
            if (candidate.collection.isNotBlank() && candidate.collection.equals(anchor.collection, ignoreCase = true)) {
                score += 40
            }
            val anchorPrimaryArtist = anchor.artist.substringBefore("•").trim().lowercase(Locale.ROOT)
            val candArtist = candidate.artist.lowercase(Locale.ROOT)
            if (anchorPrimaryArtist.length >= 4 && candArtist.contains(anchorPrimaryArtist)) {
                score += 35
            }
            val anchorWords = anchor.title.lowercase(Locale.ROOT)
                .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
                .filter { it.length >= 3 }
            val candWords = candidate.title.lowercase(Locale.ROOT)
                .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
                .filter { it.length >= 3 }
                .toSet()
            for (word in anchorWords) {
                if (word in candWords) {
                    score += if (word in devotionalKeywords) 25 else 20
                }
            }
        }

        if (query.isNotBlank()) {
            val qLower = query.lowercase(Locale.ROOT)
            if (candidate.title.lowercase(Locale.ROOT).startsWith(qLower)) score += 60
            else if (candidate.title.lowercase(Locale.ROOT).contains(qLower)) score += 40
        }

        return score
    }

    private fun areRelatedDeities(a: String, b: String): Boolean {
        if (a == b) return true
        val vaishnavGroup = setOf("rama", "ram", "krishna", "vishnu", "lakshmi", "mixed")
        val shivaShaktiGaneshGroup = setOf("shiva", "ganesh", "durga", "hanuman")
        return (a in vaishnavGroup && b in vaishnavGroup) ||
            (a == "ganesh" && b == "hanuman") ||
            (a == "hanuman" && b == "ganesh") ||
            (a == "hanuman" && b == "rama") ||
            (a == "rama" && b == "hanuman") ||
            (a in shivaShaktiGaneshGroup && b == "shiva")
    }

    private fun parseDurationSeconds(text: String): Long {
        if (text.isBlank()) return 0L
        val parts = text.trim().split(':').mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
            2 -> parts[0] * 60L + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }

    private fun inferDeity(text: String, fallback: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("hanuman") || lower.contains("bajrang") || lower.contains("हनुमान") -> "hanuman"
            lower.contains("shiv") || lower.contains("mahadev") || lower.contains("bhola") || lower.contains("शिव") -> "shiva"
            lower.contains("ganesh") || lower.contains("ganpati") || lower.contains("gajanan") || lower.contains("गणेश") -> "ganesh"
            lower.contains("durga") || lower.contains("ambe") || lower.contains("mata") || lower.contains("माता") -> "durga"
            lower.contains("krishna") || lower.contains("shyam") || lower.contains("bihari") || lower.contains("कृष्ण") -> "krishna"
            lower.contains("ram") || lower.contains("raghav") || lower.contains("राम") -> "rama"
            lower.contains("lakshmi") || lower.contains("jagdish") || lower.contains("लक्ष्मी") -> "lakshmi"
            else -> fallback.ifBlank { "mixed" }
        }
    }

    private fun unescapeJsonText(raw: String): String {
        if (raw.isEmpty()) return ""
        return raw
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
    }
}

