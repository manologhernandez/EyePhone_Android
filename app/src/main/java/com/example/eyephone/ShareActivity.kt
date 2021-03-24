package com.example.eyephone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_share.*
import java.io.File


class ShareActivity : AppCompatActivity() {
    lateinit var timestamp: String
    lateinit var imgTitle: String
    lateinit var imgType: String
    lateinit var uriString: String
    lateinit var filepath: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val extras = intent.extras

        if (extras != null) {
            val myTimestamp = extras.getString("Timestamp")
            val myUriString = extras.getString("Uri")
            val myFilepath = extras.getString("filepath")
            val myTitle = extras.getString("title")
            val myType = extras.getString("type")
            if (myTimestamp!=null){
                dateText.text = myTimestamp
                timestamp = myTimestamp
            }
            if (myTitle!=null){
                titleText.text = myTitle
                imgTitle = myTitle
            }
            if (myType!=null){
                typeText.text = myType
                imgType = myType
            }
            if (myUriString!=null){
                val uri = Uri.parse(myUriString)
                shareImg.setImageURI(uri)
                uriString = myUriString
            }
            if (myFilepath!=null){
                filepath = myFilepath
            }
        }

        shareBtn.setOnClickListener{
            println("Share button pressed")
            val uri = Uri.parse(uriString)
            val myPath = uri.path
            println("MY PATH: $myPath")
            val myFile = File(myPath)
            val photoURI = FileProvider.getUriForFile(this, this.applicationContext.packageName.toString() + ".provider", myFile)
//            val shareIntent = Intent()
//            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//            shareIntent.action = Intent.ACTION_SEND
//            shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI)
//            shareIntent.putExtra(Intent.EXTRA_TEXT, "$timestamp\nLeft Eye");
//            shareIntent.type = "image/jpeg"
//
//            val chooser = (Intent.createChooser(shareIntent, "Share Image..."))
//            startActivity(chooser)

            val bundle = Bundle()
            bundle.putString("uri", uriString)
            bundle.putString("filepath", myPath)
            bundle.putParcelable("photoUri", photoURI)
            bundle.putString("title", myFile.name)

            ShareFragment().apply {
                show(supportFragmentManager, tag)
                arguments = bundle
            }



        }
        deleteBtn.setOnClickListener{
            println("delete button pressed")
            MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Image")
                    .setMessage("You are about to delete this image. This action cannot be undone. ")
                    .setNegativeButton("Cancel") { dialog, which ->
                        // Respond to negative button press
                    }
                    .setPositiveButton("Delete") { dialog, which ->
                        // Respond to positive button press
                        val isDeleted = deleteImage(filepath)
                        println("My Filepath is $filepath")
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        intent.putExtra("showSnackbar", true)
                        if(isDeleted){
                            intent.putExtra("snackbarText", "Your image was successfully deleted.")
                        }else{
                            intent.putExtra("snackbarText", "Error deleting image.")
                        }

                        startActivity(intent);
                    }
                    .show()
        }
    }
    private fun deleteImage(filepath: String): Boolean{
        val file = File(filepath)
        if (file.exists()){
            println("FILE EXISTS!")
        }else{
            println("FILE NOT FOUND!")
        }
        return file.delete()
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}