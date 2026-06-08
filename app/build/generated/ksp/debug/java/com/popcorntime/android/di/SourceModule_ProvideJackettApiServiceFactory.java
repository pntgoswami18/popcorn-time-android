package com.popcorntime.android.di;

import com.popcorntime.android.data.api.JackettApiService;
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
public final class SourceModule_ProvideJackettApiServiceFactory implements Factory<JackettApiService> {
  private final Provider<HttpClient> clientProvider;

  public SourceModule_ProvideJackettApiServiceFactory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public JackettApiService get() {
    return provideJackettApiService(clientProvider.get());
  }

  public static SourceModule_ProvideJackettApiServiceFactory create(
      Provider<HttpClient> clientProvider) {
    return new SourceModule_ProvideJackettApiServiceFactory(clientProvider);
  }

  public static JackettApiService provideJackettApiService(HttpClient client) {
    return Preconditions.checkNotNullFromProvides(SourceModule.INSTANCE.provideJackettApiService(client));
  }
}
