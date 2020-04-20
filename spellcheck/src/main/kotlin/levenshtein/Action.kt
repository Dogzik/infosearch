package levenshtein

sealed class Action {
  data class Removal(val letter: Char) : Action()
  data class Insertion(val letter: Char) : Action()
  data class Replacement(val from: Char, val to: Char) : Action()
  data class Match(val letter: Char) : Action()

  fun getType(): ActionType = when (this) {
    is Removal -> ActionType.REMOVAL
    is Insertion -> ActionType.INSERTION
    is Replacement -> ActionType.REPLACEMENT
    is Match -> ActionType.MATCH
  }
}
