package com.popcorntime.android.di;

import com.popcorntime.android.data.cast.KodiCaster;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.ktor.client.HttpClient;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class CastModule_ProvideKodiCasterFactory implements Factory<KodiCaster> {
  private final Provider<HttpClient> httpClientProvider;

  public CastModule_ProvideKodiCasterFactory(Provider<HttpClient> httpClientProvider) {
    this.httpClientProvider = httpClientProvider;
  }

  @Override
  public KodiCaster get() {
    return provideKodiCaster(httpClientProvider.get());
  }

  public static CastModule_ProvideKodiCasterFactory create(
      Provider<HttpClient> httpClientProvider) {
    return new CastModule_ProvideKodiCasterFactory(httpClientProvider);
  }

  public static KodiCaster provideKodiCaster(HttpClient httpClient) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideKodiCaster(httpClient));
  }
}
