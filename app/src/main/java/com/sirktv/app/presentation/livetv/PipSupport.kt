package com.sirktv.app.presentation.livetv

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

fun Context.supportsPip(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

fun Activity.enterSirKTVPip() {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
        }
    }
}
