package com.popcorntime.android.di;

import com.popcorntime.android.data.subtitles.OsAuthService;
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
public final class SubtitleModule_ProvideOsAuthServiceFactory implements Factory<OsAuthService> {
  private final Provider<HttpClient> clientProvider;

  public SubtitleModule_ProvideOsAuthServiceFactory(Provider<HttpClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public OsAuthService get() {
    return provideOsAuthService(clientProvider.get());
  }

  public static SubtitleModule_ProvideOsAuthServiceFactory create(
      Provider<HttpClient> clientProvider) {
    return new SubtitleModule_ProvideOsAuthServiceFactory(clientProvider);
  }

  public static OsAuthService provideOsAuthService(HttpClient client) {
    return Preconditions.checkNotNullFromProvides(SubtitleModule.INSTANCE.provideOsAuthService(client));
  }
}
