package com.example.memorygame

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import com.example.memorygame.model.UserImageList
import com.example.memorygame.utils.EXTRA_BOARD_SIZE
import com.example.memorygame.utils.EXTRA_GAME_NAME
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var memoryGame: MemoryGame
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var rvBoard: RecyclerView
    private lateinit var adapter: MemoryBoardAdapter
    private val db = Firebase.firestore
    private var customGameImages: List<String>? = null
    private var gameName: String? = null
    private var colorNoneProgress = 0
    private var colorFullProgress = 0
    private var boardSize: BoardSize = BoardSize.EASY

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
                    showAlertDialog("Are you sure want to restart the game?", null)
                    { setupBoard() }
                } else {
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom_game -> {
                showCreationDialog()
                return true
            }
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get()
            .addOnSuccessListener { document ->
               val userImageList = document.toObject(UserImageList::class.java)
                if (userImageList?.images == null) {
                    Log.e(TAG, "Invalid game data from Firestore")
                    Snackbar.make(clRoot, "Sorry, we couldn't find any such game $customGameName", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val numCards = userImageList.images.size * 2
                boardSize = BoardSize.getByValue(numCards)
                gameName = customGameName
                customGameImages = userImageList.images
                for (imageUrl in userImageList.images) {
                    Picasso.get().load(imageUrl).fetch()
                }
                Snackbar.make(clRoot, "You're now playing '$customGameName'!", Snackbar.LENGTH_LONG).show()
                setupBoard()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Exception with getting custom game $customGameName", exception)
            }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView) {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, 200)
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView) {
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        colorNoneProgress = ContextCompat.getColor(this, R.color.color_progress_none)
        colorFullProgress = ContextCompat.getColor(this, R.color.color_progress_full)
        tvNumPairs.setTextColor(colorNoneProgress)
        tvNumPairs.text = "Pairs: 0 / ${boardSize.getNumPairs()}"
        tvNumMoves.text = "Moves: 0"
        memoryGame = MemoryGame(boardSize, customGameImages)
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
        memoryGame.flipCard(position)
        if (memoryGame.wonGame()) {
            CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.GREEN, Color.YELLOW, Color.MAGENTA)).oneShot()
            Snackbar.make(clRoot, "you won!", Snackbar.LENGTH_LONG).show()
        }
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