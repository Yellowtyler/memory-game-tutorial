package com.example.memorygame

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memorygame.model.BoardSize
import com.example.memorygame.utils.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val PICK_PHOTO_CODE: Int = 201
        private const val READ_EXTERNAL_PHOTO_CODE: Int = 200
        private const val READ_PHOTOS_PERMISSION = READ_EXTERNAL_STORAGE
        private const val TAG = "CreateActivity"
        private const val MIN_LENGTH_GAME_NAME = 3
        private const val MAX_LENGTH_GAME_NAME = 14
    }

    private lateinit var createCl: ConstraintLayout
    private lateinit var boardSize: BoardSize
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var imagePickerAdapter: ImagePickerAdapter
    private lateinit var pbUploading: ProgressBar

    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        createCl = findViewById(R.id.clCreate)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        pbUploading = findViewById(R.id.pbUploading)
        pbUploading.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0/$numImagesRequired)"
        btnSave.setOnClickListener {
            saveDataToFirebase()
        }
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_LENGTH_GAME_NAME))
        etGameName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }
        })

        imagePickerAdapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener {
            override fun onPlaceholderClicked() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    launchIntentForPhotos()
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTO_CODE)
                }
            }
        })
        rvImagePicker.adapter = imagePickerAdapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        val customGameName = etGameName.text.toString()
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document !=null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken")
                    .setMessage("A game already exists with the name '$customGameName'. Please choose another")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }

    }

    private fun handleImageUploading(customGameName: String) {
        pbUploading.visibility = View.VISIBLE
        val uploadedImagesUrls = mutableListOf<String>()
        var didEncounterError = false
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$customGameName/${System.currentTimeMillis()}-$index.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray).continueWithTask { uploadPhotoTask ->
                Log.i(TAG, "Upload bytes: ${uploadPhotoTask.result?.bytesTransferred}")
                photoReference.downloadUrl
            }.addOnCompleteListener { downloadUrlTask ->
                if (!downloadUrlTask.isSuccessful) {
                    Log.e(TAG, "Exception with firebase storage", downloadUrlTask.exception)
                    Toast.makeText(this, "Failed upload images", Toast.LENGTH_SHORT).show()
                    didEncounterError = true
                    return@addOnCompleteListener
                }

                if (didEncounterError) {
                    pbUploading.visibility = View.GONE
                    return@addOnCompleteListener
                }
                val downloadUrl = downloadUrlTask.result.toString()
                pbUploading.progress = uploadedImagesUrls.size * 100 / chosenImageUris.size
                uploadedImagesUrls.add(downloadUrl)
                Log.i(TAG, "Finished uploading images $photoUri, num uploaded ${uploadedImagesUrls.size}")
                if (uploadedImagesUrls.size == chosenImageUris.size) {
                    handleAllUploadedImages(customGameName, uploadedImagesUrls)
                }
            }
        }
    }

    private fun handleAllUploadedImages(gameName: String, imagesUrls: MutableList<String>) {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imagesUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with firebase database", gameCreationTask.exception)
                    Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload your game! Let's play your game $gameName")
                    .setPositiveButton("OK") { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == READ_EXTERNAL_PHOTO_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this,
                    "In order to create your own game, you need to provide access to your photos",
                    Toast.LENGTH_LONG)
                    .show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from launched activity")
        }
        val selectedUri = data?.data
        val clipData = data?.clipData
        if (clipData != null) {
            Log.i(TAG, "clip data images ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        imagePickerAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if (chosenImageUris.size < numImagesRequired) {
            return false
        }
        if (etGameName.text.isBlank() || etGameName.length() < MIN_LENGTH_GAME_NAME) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTO_CODE)
    }

}