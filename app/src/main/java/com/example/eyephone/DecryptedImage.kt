package com.example.eyephone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat


class DecryptedImage(val byteArray: ByteArray, val alias: String, val filePath: String, val imgTitle: String, val img_type: String, val imgDate: java.util.Date) {
    fun toBitmap (): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
    fun createTimestamp(): String{
        val simpleDateFormat = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss:S z")
        return simpleDateFormat.format(imgDate)
    }
}