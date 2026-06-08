package com.popcorntime.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.ktor.client.HttpClient;
import javax.annotation.processing.Generated;

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
public final class TraktModule_ProvideTraktHttpClientFactory implements Factory<HttpClient> {
  @Override
  public HttpClient get() {
    return provideTraktHttpClient();
  }

  public static TraktModule_ProvideTraktHttpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static HttpClient provideTraktHttpClient() {
    return Preconditions.checkNotNullFromProvides(TraktModule.INSTANCE.provideTraktHttpClient());
  }

  private static final class InstanceHolder {
    private static final TraktModule_ProvideTraktHttpClientFactory INSTANCE = new TraktModule_ProvideTraktHttpClientFactory();
  }
}
