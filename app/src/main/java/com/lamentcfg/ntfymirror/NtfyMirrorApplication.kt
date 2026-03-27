package com.lamentcfg.ntfymirror

import android.app.Application
import android.content.Context
import com.lamentcfg.ntfymirror.data.ChannelDiscoveryManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Application class with dependency injection setup using a simple service locator pattern.
 * Provides access to shared dependencies throughout the app.
 */
class NtfyMirrorApplication : Application() {

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        // Add logging interceptor for debug builds
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        // Initialize channel discovery manager
        ChannelDiscoveryManager.getInstance().initialize(this)
    }

    /**
     * Service locator providing access to app-wide dependencies.
     */
    object ServiceLocator {
        fun getOkHttpClient() = _instance.okHttpClient

        fun getContext(): Context = _instance.applicationContext
    }

    companion object {
        private lateinit var _instance: NtfyMirrorApplication

        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val WRITE_TIMEOUT_SECONDS = 30L
    }
}
