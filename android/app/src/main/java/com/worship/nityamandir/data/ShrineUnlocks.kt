package com.worship.nityamandir.data

data class ShrineUnlock(val category: ShrineCategory, val option: ShrineOption, val cost: Int) {
    val id get() = "${category.id}:${option.id}"
}

/** Pure discovery rules, independent of Android storage and account JSON. */
internal object ShrineUnlocks {
    fun cost(category: String, item: String, categoryCost: Int? = null): Int {
        if (item == "original" || (category == "shrine" && item == "marble")) return 0
        return categoryCost?.takeIf { it >= 0 } ?: when (category) {
            "shrine" -> 7
            "flowers" -> 2
            else -> 0
        }
    }

    fun seenPreferenceKey(accountId: String?): String? =
        accountId?.takeIf { it.isNotBlank() }?.let { "seen_shrine_unlocks_$it" }

    private fun paidOptions(
        categories: List<ShrineCategory>,
        cost: (String, String) -> Int
    ): List<ShrineUnlock> = categories.flatMap { category ->
        category.options.mapNotNull { option ->
            cost(category.id, option.id).takeIf { it > 0 }?.let { ShrineUnlock(category, option, it) }
        }
    }

    fun newUnlocks(
        categories: List<ShrineCategory>,
        seen: Set<String>,
        owns: (String, String) -> Boolean,
        cost: (String, String) -> Int = { category, item -> ShrineUnlocks.cost(category, item) }
    ): List<ShrineUnlock> = paidOptions(categories, cost).filter {
        it.id !in seen && owns(it.category.id, it.option.id)
    }

    fun availableUnlocks(
        categories: List<ShrineCategory>,
        coins: Int,
        owns: (String, String) -> Boolean,
        cost: (String, String) -> Int = { category, item -> ShrineUnlocks.cost(category, item) }
    ): List<ShrineUnlock> = paidOptions(categories, cost).filter {
        it.cost <= coins && !owns(it.category.id, it.option.id)
    }

    fun seenAfterViewing(
        categories: List<ShrineCategory>,
        seen: Set<String>,
        owns: (String, String) -> Boolean,
        cost: (String, String) -> Int = { category, item -> ShrineUnlocks.cost(category, item) }
    ): Set<String> = seen + newUnlocks(categories, seen, owns, cost).map { it.id }

    fun seenAfterUnlock(
        seen: Set<String>,
        category: String,
        item: String,
        owns: Boolean,
        cost: Int
    ): Set<String> = if (owns && cost > 0) seen + "$category:$item" else seen
}
