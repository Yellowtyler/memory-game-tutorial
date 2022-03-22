package com.example.memorygame.model

data class MemoryCard(
    val id: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false

)
