package com.nikolaM.soundscout.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CloudinaryService {
    private val client = OkHttpClient()

    suspend fun bitmapToTempJpeg(context: Context, bmp: Bitmap): Uri = withContext(Dispatchers.IO) {
        val file = File.createTempFile("camera_", ".jpg", context.cacheDir)
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    }

    suspend fun uploadImageUnsigned(
        context: Context,
        uri: Uri,
        cloudName: String,
        uploadPreset: String,
        folder: String
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open input stream for $uri")
        val temp = File.createTempFile("upl_", ".jpg", context.cacheDir)
        FileOutputStream(temp).use { input.copyTo(it) }

        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", temp.name, temp.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadPreset)
            .addFormDataPart("folder", folder)
            .build()

        val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
        val res = client.newCall(Request.Builder().url(url).post(body).build()).execute()
        if (!res.isSuccessful) error("Cloudinary upload failed: HTTP ${res.code}")
        val json = JSONObject(res.body!!.string())
        temp.delete()

        json.getString("secure_url") to json.getString("public_id")
    }
}
