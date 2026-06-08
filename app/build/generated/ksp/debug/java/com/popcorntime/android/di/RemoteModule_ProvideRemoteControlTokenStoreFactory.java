package com.popcorntime.android.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.popcorntime.android.data.remote.RemoteControlTokenStore;
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
public final class RemoteModule_ProvideRemoteControlTokenStoreFactory implements Factory<RemoteControlTokenStore> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public RemoteModule_ProvideRemoteControlTokenStoreFactory(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public RemoteControlTokenStore get() {
    return provideRemoteControlTokenStore(dataStoreProvider.get());
  }

  public static RemoteModule_ProvideRemoteControlTokenStoreFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new RemoteModule_ProvideRemoteControlTokenStoreFactory(dataStoreProvider);
  }

  public static RemoteControlTokenStore provideRemoteControlTokenStore(
      DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(RemoteModule.INSTANCE.provideRemoteControlTokenStore(dataStore));
  }
}
