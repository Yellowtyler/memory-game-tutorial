package com.example.memorygame

import com.example.memorygame.model.BoardSize
import com.example.memorygame.model.MemoryCard
import com.example.memorygame.utils.DEFAULT_ICONS

class MemoryGame(
    private val boardSize: BoardSize,
    customImages: List<String>?
    ) {

    private var position1: Int? = null
    private var position2: Int? = null
    private var foundPairs: Int = 0
    private var numMoves: Int = 0

    val memoryCards: List<MemoryCard> = if (customImages == null) {
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        (chosenImages + chosenImages).shuffled().map{MemoryCard(it)}
    } else {
        (customImages + customImages).shuffled().map{MemoryCard(it.hashCode(), it)}
    }


    fun flipCard(position: Int) {
        if (!memoryCards[position].isFaceUp && !memoryCards[position].isMatched) {
            memoryCards[position].isFaceUp =
                if (!memoryCards[position].isMatched) !memoryCards[position].isFaceUp else true
            if (position1 == null) {
                position1 = position
            } else if (position2 == null) {
                position2 = position
                if (memoryCards[position1!!].id == memoryCards[position2!!].id) {
                    memoryCards[position1!!].isMatched = true
                    memoryCards[position2!!].isMatched = true
                    position1 = null
                    position2 = null
                    foundPairs++
                    numMoves++

                }
            } else {
                memoryCards[position1!!].isFaceUp = false
                memoryCards[position2!!].isFaceUp = false
                position1 = position
                position2 = null
                numMoves++
            }
        }
    }

    fun wonGame(): Boolean {
        return boardSize.getNumPairs() == foundPairs
    }

    fun getFoundCards(): Int {
        return foundPairs
    }

    fun getNumMoves(): Int {
        return numMoves
    }
}