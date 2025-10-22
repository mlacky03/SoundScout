package com.nikolaM.soundscout.ui.screens.auth

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolaM.soundscout.BuildConfig
import com.nikolaM.soundscout.data.repository.AuthRepository
import com.nikolaM.soundscout.data.remote.CloudinaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = AuthRepository.currentUid() != null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val _ui = MutableStateFlow(AuthUiState())

    val ui = _ui.asStateFlow()

    fun signIn(username: String, password: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        runCatching { AuthRepository.signIn(username, password) }
            .onSuccess { AuthRepository.currentUid()?.let { uid -> AuthRepository.setUserOnlineStatus(uid, true) }
                        _ui.value = _ui.value.copy(isLoading = false, isLoggedIn = true) }
            .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
    }

    fun signUp(
        context: Context,
        username: String,
        password: String,
        name: String,
        surname: String,
        phone: String,
        galleryUri: Uri?,
        cameraBitmap: Bitmap?
    ) = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)
        runCatching {

            val photoUrl = when {
                galleryUri != null -> {
                    val (url, _) = CloudinaryService.uploadImageUnsigned(
                        context,
                        galleryUri,
                        BuildConfig.CLOUDINARY_CLOUD_NAME,
                        BuildConfig.CLOUDINARY_UPLOAD_PRESET,
                        BuildConfig.CLOUDINARY_FOLDER
                    )
                    url
                }
                cameraBitmap != null -> {
                    val tmp = CloudinaryService.bitmapToTempJpeg(context, cameraBitmap)
                    val (url, _) = CloudinaryService.uploadImageUnsigned(
                        context,
                        tmp,
                        BuildConfig.CLOUDINARY_CLOUD_NAME,
                        BuildConfig.CLOUDINARY_UPLOAD_PRESET,
                        BuildConfig.CLOUDINARY_FOLDER
                    )
                    url
                }
                else -> null
            }

            val cred = AuthRepository.preSignUp(username, password)
            val uid = cred.user?.uid
                ?: throw IllegalStateException("Kreiranje naloga nije uspelo, UID je null.")


            AuthRepository.createProfile(uid, username, name, surname, phone, photoUrl)
        }
            .onSuccess { _ui.value = _ui.value.copy(isLoading = false, isLoggedIn = true) }
            .onFailure { _ui.value = _ui.value.copy(isLoading = false, error = it.message) }
    }

    fun signOut() {
        AuthRepository.signOut()
        _ui.value = _ui.value.copy(isLoggedIn = false)
    }
}
