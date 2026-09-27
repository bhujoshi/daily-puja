package com.worship.nityamandir.engine

import java.util.Locale

/** Search titles as well as tags: Archive uploads do not share a consistent taxonomy. */
object BhajanSearch {
    private const val devotional = "(title:(bhajan OR bhajans OR aarti OR arti OR arati OR chalisa OR chalisha OR mantra OR stotra OR kirtan OR भजन OR आरती OR चालीसा OR मंत्र) OR subject:(bhajan OR bhajans OR aarti OR chalisa OR mantra OR stotra OR kirtan OR भजन OR आरती))"
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
}
