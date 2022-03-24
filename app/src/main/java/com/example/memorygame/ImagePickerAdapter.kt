package com.example.memorygame

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,
                         private val imageUris: List<Uri>,
                         private val boardSize: BoardSize) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardLength = min(cardHeight, cardWidth)
        val layoutParams = view.findViewById<ImageView>(R.id.imageView).layoutParams
        layoutParams.width = cardLength
        layoutParams.height = cardLength
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount(): Int = boardSize.getNumPairs()

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)

        fun bind(uri: Uri) {
            imageView.setImageURI(uri)
            imageView.setOnClickListener(null)
        }

        fun bind() {
            imageView.setOnClickListener {  }
        }

    }

}
