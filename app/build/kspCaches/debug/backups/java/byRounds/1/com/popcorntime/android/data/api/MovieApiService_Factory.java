package com.popcorntime.android.data.api;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.ktor.client.HttpClient;
import java.util.List;
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
public final class MovieApiService_Factory implements Factory<MovieApiService> {
  private final Provider<HttpClient> clientProvider;

  private final Provider<List<String>> serversProvider;

  public MovieApiService_Factory(Provider<HttpClient> clientProvider,
      Provider<List<String>> serversProvider) {
    this.clientProvider = clientProvider;
    this.serversProvider = serversProvider;
  }

  @Override
  public MovieApiService get() {
    return newInstance(clientProvider.get(), serversProvider.get());
  }

  public static MovieApiService_Factory create(Provider<HttpClient> clientProvider,
      Provider<List<String>> serversProvider) {
    return new MovieApiService_Factory(clientProvider, serversProvider);
  }

  public static MovieApiService newInstance(HttpClient client, List<String> servers) {
    return new MovieApiService(client, servers);
  }
}
