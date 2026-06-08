package com.popcorntime.android.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.popcorntime.android.data.trakt.TraktTokenStore;
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
public final class TraktModule_ProvideTraktTokenStoreFactory implements Factory<TraktTokenStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public TraktModule_ProvideTraktTokenStoreFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public TraktTokenStore get() {
    return provideTraktTokenStore(dataStoreProvider.get());
  }

  public static TraktModule_ProvideTraktTokenStoreFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new TraktModule_ProvideTraktTokenStoreFactory(dataStoreProvider);
  }

  public static TraktTokenStore provideTraktTokenStore(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(TraktModule.INSTANCE.provideTraktTokenStore(dataStore));
  }
}
