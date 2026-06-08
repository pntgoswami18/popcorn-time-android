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
public final class ShowApiService_Factory implements Factory<ShowApiService> {
  private final Provider<HttpClient> clientProvider;

  public ShowApiService_Factory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public ShowApiService get() {
    return newInstance(clientProvider.get());
  }

  public static ShowApiService_Factory create(Provider<HttpClient> clientProvider) {
    return new ShowApiService_Factory(clientProvider);
  }

  public static ShowApiService newInstance(HttpClient client) {
    return new ShowApiService(client);
  }
}
