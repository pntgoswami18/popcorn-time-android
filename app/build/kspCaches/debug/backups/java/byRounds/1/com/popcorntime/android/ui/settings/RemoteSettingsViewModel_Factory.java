package com.popcorntime.android.ui.settings;

import com.popcorntime.android.data.remote.RemoteControlServer;
import com.popcorntime.android.data.remote.RemoteControlTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class RemoteSettingsViewModel_Factory implements Factory<RemoteSettingsViewModel> {
  private final Provider<RemoteControlServer> serverProvider;

  private final Provider<RemoteControlTokenStore> tokenStoreProvider;

  public RemoteSettingsViewModel_Factory(Provider<RemoteControlServer> serverProvider,
      Provider<RemoteControlTokenStore> tokenStoreProvider) {
    this.serverProvider = serverProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public RemoteSettingsViewModel get() {
    return newInstance(serverProvider.get(), tokenStoreProvider.get());
  }

  public static RemoteSettingsViewModel_Factory create(Provider<RemoteControlServer> serverProvider,
      Provider<RemoteControlTokenStore> tokenStoreProvider) {
    return new RemoteSettingsViewModel_Factory(serverProvider, tokenStoreProvider);
  }

  public static RemoteSettingsViewModel newInstance(RemoteControlServer server,
      RemoteControlTokenStore tokenStore) {
    return new RemoteSettingsViewModel(server, tokenStore);
  }
}
