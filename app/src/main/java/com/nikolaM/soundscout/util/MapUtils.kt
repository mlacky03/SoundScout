package com.nikolaM.soundscout.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

@Composable
fun bitmapDescriptorFromVector(
    @DrawableRes vectorResId: Int,
    tintColor: Color
): BitmapDescriptor? {
    val context = LocalContext.current
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null

    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    val androidColor = tintColor.hashCode() // Konvertujemo Compose boju u Android boju
    DrawableCompat.setTint(vectorDrawable, androidColor)

    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    vectorDrawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}