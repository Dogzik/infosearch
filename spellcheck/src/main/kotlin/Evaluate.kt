import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import levenshtein.LevenshteinDistance
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
  val labelsPath = Paths.get("src/main/resources/data/train.csv")
  val noFixPath = Paths.get("src/main/resources/data/no_fix_train.csv")
  val predictionPath = Paths.get("src/main/resources/data/processed.csv")

  val labels = csvReader()
    .readAllWithHeader(labelsPath.toFile())
    .asSequence()
    .map { Pair(it["Id"]!!, it["Expected"]!!) }
    .toMap()

  val getScore = { path: Path ->
    csvReader().readAllWithHeader(path.toFile())
      .asSequence()
      .map { LevenshteinDistance.calculate(labels[it["Id"]!!]!!, it["Predicted"]!!).distance.toDouble() }
      .average()
  }

  val noFixScore = getScore(noFixPath)
  val predictionScore = getScore(predictionPath)
  println("No fix score: $noFixScore")
  println("Prediction score: $predictionScore")
}
