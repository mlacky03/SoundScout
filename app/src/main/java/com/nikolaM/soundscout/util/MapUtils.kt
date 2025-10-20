package com.nikolaM.soundscout.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

//@Composable
//fun bitmapDescriptorFromVector(
//    @DrawableRes vectorResId: Int,
//    tintColor: Color,
//    size: Dp = 48.dp
//): BitmapDescriptor? {
//    val context = LocalContext.current
//    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
//    val pxSize = with(LocalDensity.current) { size.toPx().toInt() }
//
//    val bitmap = Bitmap.createBitmap(
//        vectorDrawable.intrinsicWidth,
//        vectorDrawable.intrinsicHeight,
//        Bitmap.Config.ARGB_8888
//    )
//    val canvas = Canvas(bitmap)
//
//    val androidColor = tintColor.hashCode() // Konvertujemo Compose boju u Android boju
//    DrawableCompat.setTint(vectorDrawable, androidColor)
//
//    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
//    vectorDrawable.draw(canvas)
//
//    return BitmapDescriptorFactory.fromBitmap(bitmap)
//}

@Composable
fun bitmapDescriptorFromVector(
    @DrawableRes vectorResId: Int,
    tintColor: Color,
    size: Dp = 32.dp // <<-- NOVI PARAMETAR: Podrazumevana veličina
): BitmapDescriptor? {
    val context = LocalContext.current
    val pxSize = with(LocalDensity.current) { size.toPx().toInt() } // Konvertujemo dp u piksele

    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null

    vectorDrawable.setBounds(0, 0, pxSize, pxSize) // Postavljamo novu, veću veličinu

    val androidColor = tintColor.toArgb() // Konvertujemo Compose boju u Android boju
    DrawableCompat.setTint(vectorDrawable, androidColor)

    val bitmap = Bitmap.createBitmap(
        pxSize,
        pxSize,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)

    vectorDrawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}