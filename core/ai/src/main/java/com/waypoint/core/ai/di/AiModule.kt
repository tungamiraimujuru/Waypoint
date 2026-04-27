package com.waypoint.core.ai.di

import com.waypoint.core.ai.network.buildHttpClient
import com.waypoint.core.ai.orchestrator.AiOrchestrator
import com.waypoint.core.ai.orchestrator.RealAiOrchestrator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Hilt bindings for the AI layer.
 *
 * Two modules: an abstract one for interface-to-impl bindings (Binds is
 * cheaper than Provides for those), and an object-style one for objects
 * Hilt can't create with @Inject (third-party types like HttpClient).
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiBindingsModule {

    @Binds
    @Singleton
    abstract fun bindOrchestrator(impl: RealAiOrchestrator): AiOrchestrator
}

@Module
@InstallIn(SingletonComponent::class)
internal object AiProvidersModule {

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = buildHttpClient(json)
}