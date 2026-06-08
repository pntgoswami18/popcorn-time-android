package com.popcorntime.android.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.popcorntime.android.data.subtitles.OsTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SubtitleModule_ProvideOsTokenStoreFactory implements Factory<OsTokenStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public SubtitleModule_ProvideOsTokenStoreFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public OsTokenStore get() {
    return provideOsTokenStore(dataStoreProvider.get());
  }

  public static SubtitleModule_ProvideOsTokenStoreFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new SubtitleModule_ProvideOsTokenStoreFactory(dataStoreProvider);
  }

  public static OsTokenStore provideOsTokenStore(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(SubtitleModule.INSTANCE.provideOsTokenStore(dataStore));
  }
}
