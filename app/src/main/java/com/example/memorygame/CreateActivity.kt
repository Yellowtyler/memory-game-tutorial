package com.example.memorygame

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import com.example.memorygame.utils.EXTRA_BOARD_SIZE

class CreateActivity : AppCompatActivity() {
    private lateinit var createCl: ConstraintLayout
    private lateinit var boardSize: BoardSize
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var rvImagePicker: RecyclerView

    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        createCl = findViewById(R.id.clCreate)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        rvImagePicker = findViewById(R.id.rvImagePicker)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0/$numImagesRequired)"

        rvImagePicker.adapter = ImagePickerAdapter(this, chosenImageUris, boardSize)
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}