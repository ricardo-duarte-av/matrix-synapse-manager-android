package com.matrix.synapse.manager

import com.matrix.synapse.network.ActiveTokenHolder
import com.matrix.synapse.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenProvider(holder: ActiveTokenHolder): TokenProvider = TokenProvider { holder.get() }
}
