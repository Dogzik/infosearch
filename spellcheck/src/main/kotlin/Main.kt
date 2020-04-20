import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import levenshtein.Action
import levenshtein.ActionType
import levenshtein.LevenshteinDistance
import model.TrieSearcher
import model.error.ActionScorer
import model.language.Language
import model.language.SuggestionFilter
import model.language.Trie
import model.language.detectLanguage
import java.nio.file.Paths

fun main() {
  val wordsPath = Paths.get("src/main/resources/data/words.csv")
  val wordsFreq = csvReader()
    .readAllWithHeader(wordsPath.toFile())
    .asSequence()
    .filter { detectLanguage(it.getValue("Id")) == Language.RUS }
    .map { Pair(it.getValue("Id"), it.getValue("Freq").toLong()) }
    .toMap()

  val trie = Trie()
  wordsFreq.forEach { (word, _) -> trie.addWord(word) }

  val actions = mutableMapOf<ActionType, Long>()
  val detailedActions = mutableMapOf<Action, Long>()
  val contextActions = mutableMapOf<Pair<Action, Action>, Long>()
  val trainPath = Paths.get("src/main/resources/data/train.csv")
  csvReader().readAllWithHeader(trainPath.toFile())
    .asSequence()
    .filter { row -> row.values.all { it.length < 50 } }
    .forEach {
      val distance = LevenshteinDistance.calculate(it.getValue("Id"), it.getValue("Expected"))
      var prevAction: Action? = null
      for (action in distance.actions) {
        actions.compute(action.getType()) { _, cnt -> (cnt ?: 0) + 1 }
        detailedActions.compute(action) { _, cnt -> (cnt ?: 0) + 1 }
        if (prevAction != null) {
          contextActions.compute(Pair(prevAction, action)) { _, cnt -> (cnt ?: 0) + 1 }
        }
        prevAction = action
      }
    }

  val scorer = ActionScorer(actions, detailedActions, contextActions)
  val trieSearcher = TrieSearcher(trie, scorer)
  val suggestionFilter = SuggestionFilter(wordsFreq, 0.0001, 2.5)

  val testPath = Paths.get("src/main/resources/data/test.csv")
  val ansPath = Paths.get("src/main/resources/data/processed.csv")
  csvWriter().open(ansPath.toFile()) {
    writeRow(listOf("Id", "Predicted"))
    csvReader().readAllWithHeader(testPath.toFile())
      .asSequence()
      .map { it.getValue("Id") }
      .forEach {
        val correction = if (detectLanguage(it) == Language.RUS) {
          val suggestions = trieSearcher.getCandidates(it, replacements = true, insertions = false, removals = false)
          val suggestion = suggestionFilter.getBestSuggestion(it, suggestions)
          suggestion ?: it
        } else {
          it
        }
        writeRow(listOf(it, correction))
      }
  }
}
