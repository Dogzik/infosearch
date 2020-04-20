package model.language

data class TrieNode(var isTerm: Boolean, val children: MutableMap<Char, TrieNode>)
