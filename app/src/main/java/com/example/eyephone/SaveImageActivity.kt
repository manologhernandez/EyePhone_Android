 package com.example.eyephone

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_save_image.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator


 class SaveImageActivity : AppCompatActivity() {
    lateinit var imageUri: Uri
    private val chars = ('a'..'Z') + ('A'..'Z') + ('0'..'9')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_image)
//        supportActionBar?.hide()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val extras = intent.extras
        if (extras != null) {
            val uriString = extras.getString("picture")
            if (uriString!=null){
                val uri = Uri.parse(uriString)
                println("Received URI in second activity")
                imageView.setImageURI(uri)
                imageUri = uri
            }
        }

        // Initialize adapter array for eye type
        val adapter = ArrayAdapter(this, R.layout.list_item, Constants().IMAGE_TYPES)
        (dropdown.editText as? AutoCompleteTextView)?.setAdapter(adapter)

        // Initialize Placeholder Title
        imgTitleContainer.editText?.setText(generateTitle())
        imgTitleContainer.setEndIconOnClickListener {
            imgTitleContainer.editText?.setText("")
        }


        // initialize save button
        saveBtn.setOnClickListener {
            // grab image title and image type from user
            val img_title = imgTitleContainer.editText?.text.toString()
            val img_type = dropdown.editText?.text.toString()
            var validated = true
            if (img_title == ""){
                imgTitleContainer.error = "Please input a valid image title."
                validated = false
            }
            if (img_type == ""){
                dropdown.error = "Please select a valid image type."
                validated = false
            }
            if (validated) {

                // obtain bitmap from imageview
                val drawable = imageView.drawable as BitmapDrawable
                val image_bitmap = drawable.bitmap



                // embed L|R on image
                val embed = when(img_type){
                    Constants().LEFT_EYE -> BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.overlay_left_eye
                    )
                    Constants().RIGHT_EYE -> BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.overlay_right_eye
                    )
                    else -> BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.overlay_both_eyes
                    )
                }

                val newBitmap = overlay(image_bitmap, embed)
                val stream = ByteArrayOutputStream()
                if (newBitmap != null) {
                    newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                val finalImage = stream.toByteArray()
                // encrypt image byte array using android keystore
                // generate a new alias and key
                val alias = generateAlias()
                generateKey(alias)
                // encrypt imageByteArray using key alias. Returns a par of IvBytes and Encrypted Bytes
                val pair :Pair<ByteArray, ByteArray> =  encrypt(finalImage, alias)
                // convert byte arrays to strings and save as json string
                val jsonString = generateJsonString(
                        pair.first,
                        pair.second,
                        alias,
                        img_title,
                        img_type,
                        Date()
                )
                val filename = "IMG_$alias.json"

                // store data into file (JSON FORMAT)
                writeJson(filename, jsonString)


            }

        }
        // initialize retake button
        retakeBtn.setOnClickListener {
            // return to camera activity
            val intent = Intent(this, CameraActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent);
        }
        // initialize sharpen seekbar
        sharpenSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed

            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // write custom code for progress is stopped
                Toast.makeText(applicationContext, "Progress is: " + seek.progress + "%",
                        Toast.LENGTH_SHORT).show()
                val imageByteArray = readBytes(applicationContext, imageUri)
                if (imageByteArray != null) {
                    val image_bitmap = BitmapFactory.decodeByteArray(
                            imageByteArray,
                            0,
                            imageByteArray.size
                    )
                    var newBitmap = sharpen(image_bitmap, (seek.progress.toFloat()) / 100)
                    imageView.setImageBitmap(newBitmap)
                }
            }
        })
    }

    @Throws(IOException::class)
    private fun readBytes(context: Context, uri: Uri): ByteArray? =
        context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }

    fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap? {
        val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bmp1, Matrix(), null)
        canvas.drawBitmap(bmp2, 30f, 30f, null)
        bmp1.recycle()
        bmp2.recycle()
        return bmOverlay
    }
     fun sharpen(bitmap: Bitmap, multiplier: Float): Bitmap {
         val sharp = floatArrayOf(0f, (-multiplier).toFloat(), 0f, (-multiplier).toFloat(), 1 + 4f * multiplier, (-multiplier).toFloat(), 0f, (-multiplier).toFloat(), 0f)
         val newBitmap = Bitmap.createBitmap(
                 bitmap.getWidth(), bitmap.getHeight(),
                 Bitmap.Config.ARGB_8888)

         val rs = RenderScript.create(this)

         val allocIn = Allocation.createFromBitmap(rs, bitmap)
         val allocOut = Allocation.createFromBitmap(rs, newBitmap)

         val convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
         convolution.setInput(allocIn)
         convolution.setCoefficients(sharp)
         convolution.forEach(allocOut)

         allocOut.copyTo(newBitmap)
         rs.destroy()

         return newBitmap
     }

    private fun generateAlias(): String = List(16) { chars.random() }.joinToString("")
    private fun generateTitle(): String {
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy")
        val date = simpleDateFormat.format(Date())
        return "Capture - $date"
    }
    private fun generateJsonString(
            ivBytes: ByteArray,
            encryptedBytes: ByteArray,
            alias: String,
            title: String,
            type: String,
            date: Date
    ): String {
        val ivBase64Str = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        val encryptedBase64Str = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val encryptedImage = EncryptedImage(
                ivBase64Str,
                encryptedBase64Str,
                alias,
                title,
                type,
                date
        )
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(encryptedImage)
    }
    private fun writeJson(filename: String, data: String) {
        val path = this.filesDir.absolutePath
        val dir = File("$path/MyCaptures")
        if (!dir.exists()){
            dir.mkdir()
        }
        println("PATH: $dir")
        try{
            val file = File("$dir/$filename")
            file.writeText(data)
//            Toast.makeText(applicationContext, "Image saved successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("showSnackbar", true)
            intent.putExtra("snackbarText", "Image saved successfully.")
            startActivity(intent);
        }catch (exc: IOException){
            exc.printStackTrace()
            Toast.makeText(applicationContext, "Error saving image.", Toast.LENGTH_SHORT).show()
        }

    }
    private fun generateKey(alias: String){
        //generate random key
        val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }
    private fun encrypt(data: ByteArray, alias: String): Pair<ByteArray, ByteArray>{
        // get the key

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // encrypt the data
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivBytes = cipher.iv
        val encryptedBytes = cipher.doFinal(data)

        return Pair(ivBytes, encryptedBytes)
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}