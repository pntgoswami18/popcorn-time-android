package com.popcorntime.android.di;

import com.popcorntime.android.data.cast.DlnaCaster;
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
public final class CastModule_ProvideDlnaCasterFactory implements Factory<DlnaCaster> {
  private final Provider<HttpClient> httpClientProvider;

  public CastModule_ProvideDlnaCasterFactory(Provider<HttpClient> httpClientProvider) {
    this.httpClientProvider = httpClientProvider;
  }

  @Override
  public DlnaCaster get() {
    return provideDlnaCaster(httpClientProvider.get());
  }

  public static CastModule_ProvideDlnaCasterFactory create(
      Provider<HttpClient> httpClientProvider) {
    return new CastModule_ProvideDlnaCasterFactory(httpClientProvider);
  }

  public static DlnaCaster provideDlnaCaster(HttpClient httpClient) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideDlnaCaster(httpClient));
  }
}
