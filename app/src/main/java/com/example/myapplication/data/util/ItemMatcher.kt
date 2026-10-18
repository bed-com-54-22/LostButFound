package com.example.myapplication.data.util

import com.example.myapplication.data.model.ItemType
import com.example.myapplication.data.model.ReportedItem

object ItemMatcher {

    private val stopWords = setOf("a", "an", "the", "of", "in", "at", "on", "for", "with", "and", "or", "item", "my")

    private fun extractTokens(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 1 && it !in stopWords }
            .toSet()
    }

    private fun calculateTokenOverlap(tokens1: Set<String>, tokens2: Set<String>): Double {
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0
        val common = tokens1.intersect(tokens2).size
        val minSize = minOf(tokens1.size, tokens2.size)
        return common.toDouble() / minSize.toDouble()
    }

    /**
     * Finds items of opposite type (e.g. FOUND items matching a LOST item)
     * with description/name/location similarity score >= 0.35.
     */
    fun findMatches(targetItem: ReportedItem, allItems: List<ReportedItem>): List<ReportedItem> {
        val targetType = targetItem.type
        val targetNameTokens = extractTokens(targetItem.name)
        val targetLocationTokens = extractTokens(targetItem.location)

        val oppositeType = if (targetType == ItemType.LOST) ItemType.FOUND else ItemType.LOST

        return allItems.filter { candidate ->
            candidate.id != targetItem.id && candidate.type == oppositeType
        }.mapNotNull { candidate ->
            var score = 0.0

            // Category match bonus
            if (targetItem.category.equals(candidate.category, ignoreCase = true) && targetItem.category != "Other") {
                score += 0.35
            }

            // Name similarity
            val candidateNameTokens = extractTokens(candidate.name)
            val nameOverlap = calculateTokenOverlap(targetNameTokens, candidateNameTokens)
            score += nameOverlap * 0.55

            // Location similarity
            val candidateLocationTokens = extractTokens(candidate.location)
            val locationOverlap = calculateTokenOverlap(targetLocationTokens, candidateLocationTokens)
            score += locationOverlap * 0.15

            if (score >= 0.35 && (nameOverlap > 0 || (score >= 0.5))) {
                candidate to score
            } else {
                null
            }
        }.sortedByDescending { it.second }
        .map { it.first }
    }
}
