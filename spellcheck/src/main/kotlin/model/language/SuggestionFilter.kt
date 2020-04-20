package model.language

class SuggestionFilter(
  private val wordsFreq: Map<String, Long>,
  private val absoluteThreshold: Double,
  private val relativeThreshold: Double
) {
  private val TOTAL_FREQ = wordsFreq.asSequence().map { it.value.toDouble() }.sum()

  private fun getFreq(word: String) = wordsFreq.getOrDefault(word, 0)

  fun getBestSuggestion(word: String, suggestions: List<String>): String? = suggestions
    .filter { getFreq(it) / TOTAL_FREQ > absoluteThreshold }
    .filter { getFreq(it).toDouble() / getFreq(word) > relativeThreshold }
    .maxBy { e -> getFreq(e) }
}
