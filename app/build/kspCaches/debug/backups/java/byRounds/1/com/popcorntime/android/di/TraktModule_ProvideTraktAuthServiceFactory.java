package com.popcorntime.android.di;

import com.popcorntime.android.data.trakt.TraktAuthService;
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
public final class TraktModule_ProvideTraktAuthServiceFactory implements Factory<TraktAuthService> {
  private final Provider<HttpClient> clientProvider;

  public TraktModule_ProvideTraktAuthServiceFactory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public TraktAuthService get() {
    return provideTraktAuthService(clientProvider.get());
  }

  public static TraktModule_ProvideTraktAuthServiceFactory create(
      Provider<HttpClient> clientProvider) {
    return new TraktModule_ProvideTraktAuthServiceFactory(clientProvider);
  }

  public static TraktAuthService provideTraktAuthService(HttpClient client) {
    return Preconditions.checkNotNullFromProvides(TraktModule.INSTANCE.provideTraktAuthService(client));
  }
}
