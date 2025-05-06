package com.example.disastermesh.core.net

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetModule {

    // ---- low‑level singletons -----------------------------------------

    @Provides @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun gson(): Gson = GsonBuilder().serializeNulls().create()

    @Provides @Singleton
    fun retrofit(
        client: OkHttpClient,
        gson:   Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://132.145.67.221:8443")          // TODO buildConfig
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // ---- high‑level service(s) ----------------------------------------

    @Provides @Singleton
    fun registerApi(retrofit: Retrofit): RegisterApi =
        retrofit.create(RegisterApi::class.java)
}