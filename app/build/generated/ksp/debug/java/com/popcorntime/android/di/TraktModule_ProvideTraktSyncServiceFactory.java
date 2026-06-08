package com.popcorntime.android.di;

import com.popcorntime.android.data.trakt.TraktSyncService;
import com.popcorntime.android.data.trakt.TraktTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.ktor.client.HttpClient;
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
public final class TraktModule_ProvideTraktSyncServiceFactory implements Factory<TraktSyncService> {
  private final Provider<HttpClient> clientProvider;

  private final Provider<TraktTokenStore> tokenStoreProvider;

  public TraktModule_ProvideTraktSyncServiceFactory(Provider<HttpClient> clientProvider,
      Provider<TraktTokenStore> tokenStoreProvider) {
    this.clientProvider = clientProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public TraktSyncService get() {
    return provideTraktSyncService(clientProvider.get(), tokenStoreProvider.get());
  }

  public static TraktModule_ProvideTraktSyncServiceFactory create(
      Provider<HttpClient> clientProvider, Provider<TraktTokenStore> tokenStoreProvider) {
    return new TraktModule_ProvideTraktSyncServiceFactory(clientProvider, tokenStoreProvider);
  }

  public static TraktSyncService provideTraktSyncService(HttpClient client,
      TraktTokenStore tokenStore) {
    return Preconditions.checkNotNullFromProvides(TraktModule.INSTANCE.provideTraktSyncService(client, tokenStore));
  }
}
