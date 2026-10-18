package com.app.aisheildme.bytrix.di

import android.content.Context
import com.app.aisheildme.bytrix.BuildConfig
import com.app.aisheildme.bytrix.data.local.SessionStore
import com.app.aisheildme.bytrix.data.remote.AuthInterceptor
import com.app.aisheildme.bytrix.data.remote.ErrorMappingInterceptor
import com.app.aisheildme.bytrix.data.remote.SafeConverterFactory
import com.app.aisheildme.bytrix.data.remote.UnauthorizedInterceptor
import com.app.aisheildme.bytrix.data.remote.api.AuthApi
import com.app.aisheildme.bytrix.data.repository.DeviceTokenProvider
import com.app.aisheildme.bytrix.data.FirebaseDeviceTokenProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true

        encodeDefaults = true

        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideSessionStore(
        @ApplicationContext context: Context,
        json: Json,
    ): SessionStore = SessionStore(context, json)

    @Provides
    @Singleton
    fun provideOkHttp(
        sessionStore: SessionStore,
        json: Json,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(sessionStore))
        .addInterceptor(ErrorMappingInterceptor(json))
        .addInterceptor(UnauthorizedInterceptor(sessionStore))
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    },
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(
            SafeConverterFactory(json.asConverterFactory("application/json".toMediaType())),
        )
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindsModule {

    @Binds
    @Singleton
    abstract fun bindDeviceTokenProvider(
        impl: FirebaseDeviceTokenProvider,
    ): DeviceTokenProvider
}
