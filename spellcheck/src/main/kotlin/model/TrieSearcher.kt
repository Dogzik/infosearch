package model

import levenshtein.Action
import model.error.ActionScorer
import model.language.Trie
import model.language.TrieNode

class TrieSearcher(
  private val trie: Trie,
  private val scorer: ActionScorer,
  private val maxChanges: Int = 1,
  private val maxActions: Int = 10
) {
  private fun getActionsForEdge(
    curInputLetter: Char?,
    curExpectedLetter: Char?,
    replacements: Boolean,
    insertions: Boolean,
    removals: Boolean
  ): List<Action> {
    val res = mutableListOf<Action>()
    if ((curInputLetter != null) && (curInputLetter == curExpectedLetter)) {
      res.add(Action.Match(curInputLetter))
    }
    if (removals && (curInputLetter != null)) {
      res.add(Action.Removal(curInputLetter))
    }
    if (insertions && (curExpectedLetter != null)) {
      res.add(Action.Insertion(curExpectedLetter))
    }
    if (replacements && (curInputLetter != null) && (curExpectedLetter != null) && (curInputLetter != curExpectedLetter)) {
      res.add(Action.Replacement(curInputLetter, curExpectedLetter))
    }
    return res
  }

  private fun getActions(
    input: String,
    inputPos: Int,
    curNode: TrieNode,
    replacements: Boolean,
    insertions: Boolean,
    removals: Boolean
  ): List<Action> {
    val res = mutableListOf<Action>()
    if (inputPos != input.length) {
      res.addAll(
        curNode.children
          .asSequence()
          .map { getActionsForEdge(input[inputPos], it.key, replacements, insertions, removals) }
          .flatten()
          .toList()
      )
      res.addAll(getActionsForEdge(input[inputPos], null, replacements, insertions, removals))
    }
    res.addAll(
      curNode.children
        .asSequence()
        .map { getActionsForEdge(null, it.key, replacements, insertions, removals) }
        .flatten()
        .toList()
    )
    res.addAll(getActionsForEdge(null, null, replacements, insertions, removals))
    return res.distinct()
  }

  private fun doGetCandidates(
    input: String,
    inputPos: Int,
    curNode: TrieNode,
    curWord: String,
    curChanges: Int,
    prevAction: Action?,
    endingSize: Int,
    replacements: Boolean,
    insertions: Boolean,
    removals: Boolean
  ): List<String> {
    val res = mutableListOf<String>()
    if ((inputPos == input.length) && curNode.isTerm) {
      res.add(curWord)
    }
    val canChange = (curChanges < maxChanges) && (input.length - inputPos > endingSize)
    val candidateActions = getActions(
      input,
      inputPos,
      curNode,
      replacements && canChange,
      insertions && canChange,
      removals && canChange
    )
    val actions = scorer.getBestActions(candidateActions, prevAction, maxActions)
    for (action in actions) {
      when (action) {
        is Action.Match -> {
          val newPos = inputPos + 1
          val newNode = curNode.children.getValue(action.letter)
          val newWord = curWord + action.letter
          res.addAll(
            doGetCandidates(
              input,
              newPos,
              newNode,
              newWord,
              curChanges,
              prevAction,
              endingSize,
              replacements,
              insertions,
              removals
            )
          )
        }
        is Action.Replacement -> {
          val newPos = inputPos + 1
          val newNode = curNode.children.getValue(action.to)
          val newWord = curWord + action.to
          val newChanges = curChanges + 1
          res.addAll(
            doGetCandidates(
              input,
              newPos,
              newNode,
              newWord,
              newChanges,
              action,
              endingSize,
              replacements,
              insertions,
              removals
            )
          )
        }
        is Action.Insertion -> {
          val newNode = curNode.children.getValue(action.letter)
          val newWord = curWord + action.letter
          val newChanges = curChanges + 1
          res.addAll(
            doGetCandidates(
              input,
              inputPos,
              newNode,
              newWord,
              newChanges,
              action,
              endingSize,
              replacements,
              insertions,
              removals
            )
          )
        }
        is Action.Removal -> {
          val newPos = inputPos + 1
          val newChanges = curChanges + 1
          res.addAll(
            doGetCandidates(
              input,
              newPos,
              curNode,
              curWord,
              newChanges,
              action,
              endingSize,
              replacements,
              insertions,
              removals
            )
          )
        }
      }
    }
    return res
  }

  fun getCandidates(word: String, replacements: Boolean, insertions: Boolean, removals: Boolean): List<String> {
    val endingSize = when {
      word.length > 6 -> {
        3
      }
      word.length > 4 -> {
        2
      }
      else -> {
        1
      }
    }
    return doGetCandidates(word, 0, trie.root, "", 0, null, endingSize, replacements, insertions, removals)
  }
}
