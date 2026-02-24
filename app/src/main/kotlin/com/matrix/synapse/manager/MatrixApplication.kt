package com.matrix.synapse.manager

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MatrixApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Use a single shared ImageLoader so memory + disk cache are shared across the app
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .crossfade(true)
                .build()
        }
    }
}
