package com.example.disastermesh.core.ble

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BleModule {
    @Provides
    fun provideBleScanner(
        impl: AndroidBleScanner
    ): BleScanner = impl
}