package com.popcorntime.android.data.remote;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class RemoteControlTokenStore_Factory implements Factory<RemoteControlTokenStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public RemoteControlTokenStore_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public RemoteControlTokenStore get() {
    return newInstance(dataStoreProvider.get());
  }

  public static RemoteControlTokenStore_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new RemoteControlTokenStore_Factory(dataStoreProvider);
  }

  public static RemoteControlTokenStore newInstance(DataStore<Preferences> dataStore) {
    return new RemoteControlTokenStore(dataStore);
  }
}
