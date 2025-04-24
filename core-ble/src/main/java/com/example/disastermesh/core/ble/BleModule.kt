package com.example.disastermesh.core.ble

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
// Ensures one instance of the bleScanner
@InstallIn(SingletonComponent::class)

/**
Uses hilt to provide the BleScanner implementation
Hilt is a dependency injection library that simplifies the process of providing dependencies
 */
object BleModule {
    @Provides
    fun provideBleScanner(
        impl: AndroidBleScanner
    ): BleScanner = impl
}

