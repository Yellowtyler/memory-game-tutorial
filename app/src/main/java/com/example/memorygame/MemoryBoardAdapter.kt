package com.example.memorygame

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import com.example.memorygame.model.MemoryCard
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val memoryCards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
        const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    override fun getItemCount(): Int = boardSize.numCards

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getWidth() - (2*MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2*MARGIN_SIZE)
        val cardLength = min(cardWidth, cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.height = cardLength
        layoutParams.width = cardLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val card = memoryCards[position]
            imageButton.setImageResource(if (card.isFaceUp) card.id else R.drawable.ic_launcher_background)
            imageButton.alpha = if (card.isMatched) .4f else 1.0f
            val colorStateList = if (card.isMatched) ContextCompat.getColorStateList(context, R.color.color_grey) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
