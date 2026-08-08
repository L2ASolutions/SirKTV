package com.sirktv.app.presentation.livetv

import com.sirktv.app.domain.model.Category

/** One country entry in the Live TV / Sports browse sidebar. */
data class CountryGroup(
    val code: String,
    val name: String,
    val categories: List<Category>
)

private val KNOWN_COUNTRY_NAMES = mapOf(
    "US" to "United States", "USA" to "United States", "UK" to "United Kingdom", "GB" to "United Kingdom",
    "CA" to "Canada", "MX" to "Mexico", "FR" to "France", "DE" to "Germany", "ES" to "Spain", "IT" to "Italy",
    "BR" to "Brazil", "AR" to "Argentina", "IN" to "India", "PK" to "Pakistan", "TR" to "Turkey",
    "NL" to "Netherlands", "PL" to "Poland", "PT" to "Portugal", "AU" to "Australia", "NZ" to "New Zealand",
    "AE" to "United Arab Emirates", "SA" to "Saudi Arabia", "EG" to "Egypt", "GR" to "Greece", "RO" to "Romania",
    "RU" to "Russia", "SE" to "Sweden", "NO" to "Norway", "DK" to "Denmark", "FI" to "Finland", "CH" to "Switzerland",
    "AT" to "Austria", "BE" to "Belgium", "IE" to "Ireland", "ZA" to "South Africa", "PH" to "Philippines",
    "ID" to "Indonesia", "MY" to "Malaysia", "SG" to "Singapore", "JP" to "Japan", "KR" to "South Korea",
    "CN" to "China", "AL" to "Albania", "RS" to "Serbia", "HR" to "Croatia", "BG" to "Bulgaria"
)

private val SEPARATOR_REGEX = Regex("""[|\-:•/]""")

/**
 * Best-effort country grouping for the Live TV browse sidebar. Xtream
 * category names have no structured country field, so this reads the
 * leading token most IPTV providers already use as a country prefix (e.g.
 * "US | News", "UK: Sports"). Anything that doesn't parse cleanly falls
 * under a single "Other" bucket rather than guessing further.
 */
fun groupCategoriesByCountry(categories: List<Category>): List<CountryGroup> {
    val buckets = LinkedHashMap<String, MutableList<Category>>()
    categories.forEach { category ->
        val token = category.name.split(SEPARATOR_REGEX).firstOrNull()?.trim().orEmpty()
        val code = token.filter { it.isLetter() }.take(3).uppercase()
        val key = if (code.length in 2..3) code else "OTHER"
        buckets.getOrPut(key) { mutableListOf() }.add(category)
    }
    return buckets.entries
        .sortedWith(compareBy({ it.key == "OTHER" }, { it.key }))
        .map { (code, categoriesInGroup) ->
            val name = if (code == "OTHER") "Other" else KNOWN_COUNTRY_NAMES[code] ?: code
            CountryGroup(code = if (code == "OTHER") "•" else code, name = name, categories = categoriesInGroup)
        }
}
