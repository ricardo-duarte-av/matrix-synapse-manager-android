package com.matrix.synapse.feature.servers.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

private val Context.serversDataStore by preferencesDataStore(name = "servers")

@Module
@InstallIn(SingletonComponent::class)
object ServersDataStoreModule {

    @Provides
    @Singleton
    @Named("servers_datastore")
    fun provideServersDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.serversDataStore
}
