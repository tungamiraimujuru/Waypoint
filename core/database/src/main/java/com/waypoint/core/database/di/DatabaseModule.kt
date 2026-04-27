package com.waypoint.core.database.di

import android.content.Context
import androidx.room.Room
import com.waypoint.core.database.WayPointDatabase
import com.waypoint.core.database.dao.ItineraryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WayPointDatabase =
        Room.databaseBuilder(
            context = context.applicationContext,
            klass = WayPointDatabase::class.java,
            name = WayPointDatabase.DB_NAME
        ).build()

    @Provides
    fun provideItineraryDao(db: WayPointDatabase): ItineraryDao = db.itineraryDao()
}