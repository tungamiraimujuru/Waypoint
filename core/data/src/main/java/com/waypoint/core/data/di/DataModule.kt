package com.waypoint.core.data.di

import com.waypoint.core.data.InMemoryItineraryRepository
import com.waypoint.core.data.ItineraryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindItineraryRepository(
        impl: InMemoryItineraryRepository
    ): ItineraryRepository
}