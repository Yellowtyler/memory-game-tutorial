package com.example.memorygame

import com.example.memorygame.model.BoardSize
import com.example.memorygame.model.MemoryCard
import com.example.memorygame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize) {

    fun flipCard(position: Int) {
        if (!memoryCards[position].isFaceUp && !memoryCards[position].isMatched) {
            memoryCards[position].isFaceUp = if (!memoryCards[position].isMatched) !memoryCards[position].isFaceUp else true
            val cards = getFaceUpCards()
            when (cards.size) {
                2 -> {
                    if (cards[0].id == cards[1].id) {
                        cards[0].isMatched = true
                        cards[1].isMatched = true
                    }
                }
                3 -> {
                    cards.forEach {it.isFaceUp = false}
                }
            }
        }
    }

    private fun getFaceUpCards(): List<MemoryCard> {
       return memoryCards.filter { it.isFaceUp && !it.isMatched }
    }

    val memoryCards: List<MemoryCard>

    init {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        memoryCards = (chosenImages + chosenImages).shuffled().map{MemoryCard(it)}
    }
}