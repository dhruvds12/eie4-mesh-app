package com.example.disastermesh.core.database

import android.content.Context
import androidx.room.Room
import com.example.disastermesh.core.database.dao.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): MeshDb =
        Room.databaseBuilder(ctx, MeshDb::class.java, "mesh.db")
            .fallbackToDestructiveMigration(false)     // dev-time convenience
            .build()

    @Provides
    fun provideChatDao(db: MeshDb): ChatDao = db.chatDao()
}