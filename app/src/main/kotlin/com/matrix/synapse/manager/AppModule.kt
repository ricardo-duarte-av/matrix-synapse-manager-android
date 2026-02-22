package com.matrix.synapse.manager

import com.matrix.synapse.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the [TokenProvider] for the current session.
     *
     * In V1 the navigation host is not yet wired, so this returns null (no active server).
     * In V2 this will be replaced with a binding that reads the active server's access token
     * from [com.matrix.synapse.security.SecureTokenStore] based on the selected server ID.
     */
    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider = TokenProvider { null }
}
