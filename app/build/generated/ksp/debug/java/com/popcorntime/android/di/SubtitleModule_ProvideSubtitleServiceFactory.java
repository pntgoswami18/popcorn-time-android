package com.popcorntime.android.di;

import com.popcorntime.android.data.subtitles.OsTokenStore;
import com.popcorntime.android.data.subtitles.SubtitleService;
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
public final class SubtitleModule_ProvideSubtitleServiceFactory implements Factory<SubtitleService> {
  private final Provider<HttpClient> clientProvider;

  private final Provider<OsTokenStore> osTokenStoreProvider;

  public SubtitleModule_ProvideSubtitleServiceFactory(Provider<HttpClient> clientProvider,
      Provider<OsTokenStore> osTokenStoreProvider) {
    this.clientProvider = clientProvider;
    this.osTokenStoreProvider = osTokenStoreProvider;
  }

  @Override
  public SubtitleService get() {
    return provideSubtitleService(clientProvider.get(), osTokenStoreProvider.get());
  }

  public static SubtitleModule_ProvideSubtitleServiceFactory create(
      Provider<HttpClient> clientProvider, Provider<OsTokenStore> osTokenStoreProvider) {
    return new SubtitleModule_ProvideSubtitleServiceFactory(clientProvider, osTokenStoreProvider);
  }

  public static SubtitleService provideSubtitleService(HttpClient client,
      OsTokenStore osTokenStore) {
    return Preconditions.checkNotNullFromProvides(SubtitleModule.INSTANCE.provideSubtitleService(client, osTokenStore));
  }
}
