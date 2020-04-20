package model.language

class Trie {
  val root = TrieNode(false, mutableMapOf())

  fun addWord(word: String) {
    var curNode = root
    word.forEach {
      curNode = curNode.children.getOrPut(it, { TrieNode(false, mutableMapOf()) })
    }
    curNode.isTerm = true
  }
}
