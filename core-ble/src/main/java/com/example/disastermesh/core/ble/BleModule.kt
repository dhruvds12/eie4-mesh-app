package com.example.disastermesh.core.ble

import com.example.disastermesh.core.ble.repository.BleMeshRepository
import com.example.disastermesh.core.ble.repository.MeshRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
// Ensures one instance of the bleScanner
@InstallIn(SingletonComponent::class)

/**
Uses hilt to provide the BleScanner implementation
Hilt is a dependency injection library that simplifies the process of providing dependencies
 */
abstract class BleModule {

    @Binds
    abstract fun bindScanner(impl: AndroidBleScanner): BleScanner

    @Binds
    abstract fun bindGattManager(impl: AndroidGattManager): GattManager

    @Binds @Singleton                 // ← add annotation, keep this binding
    abstract fun bindMeshRepo(impl: BleMeshRepository): MeshRepository
}
