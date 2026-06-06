package com.popcorntime.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.popcorntime.android.data.cast.CastManager
import com.popcorntime.android.data.cast.ChromecastCaster
import com.popcorntime.android.data.cast.DlnaCaster
import com.popcorntime.android.data.cast.DlnaDiscovery
import com.popcorntime.android.data.cast.KodiCaster
import com.popcorntime.android.data.cast.KodiPrefsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Named
import javax.inject.Singleton

private val Context.castDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "cast_prefs")

@Module
@InstallIn(SingletonComponent::class)
object CastModule {

    @Provides @Singleton @Named("castDataStore")
    fun provideCastDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.castDataStore

    @Provides @Singleton
    fun provideKodiPrefsStore(@Named("castDataStore") ds: DataStore<Preferences>): KodiPrefsStore =
        KodiPrefsStore(ds)

    @Provides @Singleton
    fun provideKodiCaster(httpClient: HttpClient): KodiCaster =
        KodiCaster(httpClient)

    @Provides @Singleton
    fun provideDlnaCaster(httpClient: HttpClient): DlnaCaster =
        DlnaCaster(httpClient)

    @Provides @Singleton
    fun provideDlnaDiscovery(@ApplicationContext context: Context): DlnaDiscovery =
        DlnaDiscovery(context)

    @Provides @Singleton
    fun provideChromecastCaster(@ApplicationContext context: Context): ChromecastCaster =
        ChromecastCaster(context)

    @Provides @Singleton
    fun provideCastManager(
        @ApplicationContext context: Context,
        kodiCaster: KodiCaster,
        dlnaCaster: DlnaCaster,
        dlnaDiscovery: DlnaDiscovery,
        chromeCaster: ChromecastCaster,
    ): CastManager = CastManager(context, kodiCaster, dlnaCaster, dlnaDiscovery, chromeCaster)
}
