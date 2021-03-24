package com.example.eyephone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.myToolbar
import java.io.BufferedReader
import java.io.File
import java.lang.Exception
import java.security.KeyStore
import java.sql.Date
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class MainActivity : AppCompatActivity(),OnItemClickListener {
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_toolbar_home_menu, menu);
        return true
    }
    override fun onItemClicked(image: DecryptedImage) {
        //clear cache
        this.cacheDir.deleteRecursively()
        //create temp file to show image
        val outputDir: File = this.cacheDir // context being the Activity pointer
        val fileName = "img_${SystemClock.uptimeMillis()}.jpg"
        File(outputDir, fileName).writeBitmap(image.toBitmap(), Bitmap.CompressFormat.JPEG, 100)
        val uri = Uri.fromFile(File("$outputDir/${fileName}"))
        val intent = Intent(this, ShareActivity::class.java)
        intent.putExtra("Timestamp", image.createTimestamp())
        intent.putExtra("Uri", uri.toString())
        intent.putExtra("filepath", image.filePath)
        intent.putExtra("title", image.imgTitle)
        intent.putExtra("type", image.img_type)

        startActivity(intent)
    }
    fun checkIntent(intent: Intent, view: View, fab:FloatingActionButton){
        val extras = intent.extras
        if (extras!=null){
            if (extras.getBoolean("showSnackbar")){
                val text = extras.getString("snackbarText")
                if (text != null){
                    Snackbar.make(fab, text, Snackbar.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(myToolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
        checkIntent(intent, mainView, fabOpenCamera)

        fabOpenCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent);
        }

        val path = "${this.filesDir.absolutePath}/MyCaptures"
        val dir = File(path)
        val files = dir.listFiles()
        val imageList = ArrayList<DecryptedImage>()

        if(files != null){
            for (file in files) {
                // read json string of file
                val data = file.bufferedReader().use { it.readText() }
                // create encryptedimage object from json string
                val gson = Gson()
                val encryptedImage = gson.fromJson(data, EncryptedImage::class.java)
                // base64decode
                val ivBytes = Base64.decode(encryptedImage.ivString, Base64.NO_WRAP)
                val encryptedBytes = Base64.decode(encryptedImage.encryptedStr, Base64.NO_WRAP)
                val alias = encryptedImage.keyAlias
                val img_title = encryptedImage.imgTitle
                val img_type = encryptedImage.imgType
                val img_date = encryptedImage.dateTaken
                // decrypt
                val decryptedBytes = decrypt(ivBytes, encryptedBytes, alias)
                // create decryptedImage object
                val filePath = this.filesDir.absolutePath + "/MyCaptures/IMG_$alias.json"
                val decryptedImage = DecryptedImage(decryptedBytes, alias, filePath, img_title, img_type, img_date)
                imageList.add(decryptedImage)
            }
            // sort image list

            imageList.sortByDescending { it.imgDate }


            //setup viewpager AND tab layout
            if (imageList.size == 0) {
                home_textview.visibility = View.VISIBLE
            }else{
                home_textview.visibility = View.GONE
            }

            viewpager.adapter = HomeAdapter(this, this, imageList)
            val tabIcons = listOf<Int>(R.drawable.ic_sharp_grid_on_24, R.drawable.ic_baseline_list_alt_24)

            TabLayoutMediator(myTabBar, viewpager) { tab, position ->
                viewpager.setCurrentItem(tab.position, true)
                tab.icon = getDrawable(tabIcons[position])
            }.attach()

        }
    }

    private fun decrypt(ivBytes:ByteArray, encryptedBytes: ByteArray, alias: String): ByteArray {
        // get the key
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        val secretKey = secretKeyEntry.secretKey

        // decrypt the data
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            return cipher.doFinal(encryptedBytes)
        }catch (exc: Exception){
            println("Exception found. Message: ${exc.message}")
        }
        return ByteArray(0)

    }
    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }
}
