package com.popcorntime.android.data.subtitles;

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
public final class SubtitleService_Factory implements Factory<SubtitleService> {
  private final Provider<HttpClient> clientProvider;

  public SubtitleService_Factory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public SubtitleService get() {
    return newInstance(clientProvider.get());
  }

  public static SubtitleService_Factory create(Provider<HttpClient> clientProvider) {
    return new SubtitleService_Factory(clientProvider);
  }

  public static SubtitleService newInstance(HttpClient client) {
    return new SubtitleService(client);
  }
}
