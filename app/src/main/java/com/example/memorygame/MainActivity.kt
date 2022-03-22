package com.example.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var memoryGame: MemoryGame
    private lateinit var clRoot: ConstraintLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var rvBoard: RecyclerView
    private lateinit var adapter: MemoryBoardAdapter
    private var colorNoneProgress = 0
    private var colorFullProgress = 0
    private val boardSize: BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.wonGame()) {
                    showAlertDialog("Are you sure want to restart the game?", null, View.OnClickListener { setupBoard() })
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNewSizeDialog() {
        LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        showAlertDialog("Choose new size", null, View.OnClickListener {

        })
    }

    @SuppressLint("SetTextI18n")
    private fun setupBoard() {
        colorNoneProgress = ContextCompat.getColor(this, R.color.color_progress_none)
        colorFullProgress = ContextCompat.getColor(this, R.color.color_progress_full)
        tvNumPairs.setTextColor(colorNoneProgress)
        tvNumPairs.text = "Pairs: 0 / ${boardSize.getNumPairs()}"
        tvNumMoves.text = "Moves: 0"
        memoryGame = MemoryGame(boardSize)
        rvBoard = findViewById(R.id.rvBoard)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.memoryCards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ -> positiveClickListener.onClick(null) }
            .show()
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.wonGame()) {
            Snackbar.make(clRoot, "you won!", Snackbar.LENGTH_LONG).show()
        }
        memoryGame.flipCard(position)
        val colorProgress = ArgbEvaluator().evaluate(
            memoryGame.getFoundCards().toFloat() / boardSize.getNumPairs(),
            colorNoneProgress,
            colorFullProgress
        ) as Int
        tvNumPairs.setTextColor(colorProgress)
        tvNumPairs.text = "Pairs: ${memoryGame.getFoundCards()} / ${boardSize.getNumPairs()}"
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}