package com.waypoint.core.data.di

import com.waypoint.core.data.ItineraryRepository
import com.waypoint.core.data.RoomItineraryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindItineraryRepository(impl: RoomItineraryRepository): ItineraryRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidersModule {

    /**
     * Json instance for converting domain types to/from storage.
     * Same configuration as the AI module's Json — kept duplicate
     * to avoid forcing a shared utility module just for this.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
}
