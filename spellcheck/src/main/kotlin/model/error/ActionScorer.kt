package model.error

import levenshtein.Action
import levenshtein.ActionType

class ActionScorer(
  private val actions: Map<ActionType, Long>,
  private val detailedActions: Map<Action, Long>,
  private val contextActions: Map<Pair<Action, Action>, Long>,
  private val ALPHA: Double = 0.0,
  private val BETA: Double = 1.0,
  private val GAMMA: Double = 0.0
) {
  private val TOTAL_ACTIONS: Double = actions.asSequence().map { it.value.toDouble() }.sum()
  private val TOTAL_DETAILED_ACTIONS: Double = detailedActions.asSequence().map { it.value.toDouble() }.sum()
  private val TOTAL_CONTEXT_ACTIONS: Double = contextActions.asSequence().map { it.value.toDouble() }.sum()

  private fun rateAction(action: Action, prevAction: Action?): Double {
    val actionScore = actions.getOrDefault(action.getType(), 0L) / TOTAL_ACTIONS
    val detailedActionScore = detailedActions.getOrDefault(action, 0L) / TOTAL_DETAILED_ACTIONS
    val contextActionRawScore = prevAction?.let { contextActions.getOrDefault(Pair(it, action), 0L) } ?: 0
    val contextActionScore = contextActionRawScore / TOTAL_CONTEXT_ACTIONS
    return ALPHA * actionScore + BETA * detailedActionScore + GAMMA * contextActionScore
  }

  fun getBestActions(actions: List<Action>, prevAction: Action?, cnt: Int): List<Action> =
    actions.map { Pair(rateAction(it, prevAction), it) }
      .sortedByDescending { it.first }
      .take(cnt)
      .map { it.second }
}
