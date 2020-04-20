package levenshtein

data class LevenshteinDistance(val distance: Int, val actions: List<Action>) {
  companion object {
    private fun Boolean.toInt(): Int = if (this) {
      1
    } else {
      0
    }

    fun calculate(from: String, to: String): LevenshteinDistance {
      val n = from.length
      val m = to.length
      val dp = Array(n + 1) { IntArray(m + 1) { 0 } }
      // delete prefix of size i
      for (i in 0..n) {
        dp[i][0] = i
      }
      // add prefix of size j
      for (j in 0..m) {
        dp[0][j] = j
      }
      for (i in 1..n) {
        for (j in 1..m) {
          val replacePrice = (from[i - 1] != to[j - 1]).toInt()
          dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + replacePrice)
        }
      }
      val actions = ArrayList<Action>()
      var i = n
      var j = m
      while (i > 0 && j > 0) {
        val replacementPrice = (from[i - 1] != to[j - 1]).toInt()
        val removalCost = dp[i - 1][j] + 1
        val insertionCost = dp[i][j - 1] + 1
        when {
          removalCost <= minOf(insertionCost, dp[i - 1][j - 1] + replacementPrice) -> {
            actions.add(Action.Removal(from[i - 1]))
            --i
          }
          insertionCost <= minOf(removalCost, dp[i - 1][j - 1] + replacementPrice) -> {
            actions.add(Action.Insertion(to[j - 1]))
            --j
          }
          else -> {
            if (from[i - 1] != to[j - 1]) {
              actions.add(Action.Replacement(from[i - 1], to[j - 1]))
            } else {
              actions.add(Action.Match(from[i - 1]))
            }
            --i
            --j
          }
        }
      }
      actions.reverse()
      return LevenshteinDistance(dp[n][m], actions)
    }
  }
}



