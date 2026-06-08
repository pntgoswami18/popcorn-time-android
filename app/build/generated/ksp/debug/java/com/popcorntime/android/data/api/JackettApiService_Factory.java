package com.popcorntime.android.data.api;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class JackettApiService_Factory implements Factory<JackettApiService> {
  private final Provider<HttpClient> clientProvider;

  public JackettApiService_Factory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public JackettApiService get() {
    return newInstance(clientProvider.get());
  }

  public static JackettApiService_Factory create(Provider<HttpClient> clientProvider) {
    return new JackettApiService_Factory(clientProvider);
  }

  public static JackettApiService newInstance(HttpClient client) {
    return new JackettApiService(client);
  }
}
