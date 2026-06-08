package com.popcorntime.android.ui.settings;

import com.popcorntime.android.data.trakt.TraktAuthService;
import com.popcorntime.android.data.trakt.TraktTokenStore;
import com.popcorntime.android.domain.repository.LibraryRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class TraktSettingsViewModel_Factory implements Factory<TraktSettingsViewModel> {
  private final Provider<TraktAuthService> traktAuthServiceProvider;

  private final Provider<TraktTokenStore> traktTokenStoreProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  public TraktSettingsViewModel_Factory(Provider<TraktAuthService> traktAuthServiceProvider,
      Provider<TraktTokenStore> traktTokenStoreProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    this.traktAuthServiceProvider = traktAuthServiceProvider;
    this.traktTokenStoreProvider = traktTokenStoreProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
  }

  @Override
  public TraktSettingsViewModel get() {
    return newInstance(traktAuthServiceProvider.get(), traktTokenStoreProvider.get(), libraryRepositoryProvider.get());
  }

  public static TraktSettingsViewModel_Factory create(
      Provider<TraktAuthService> traktAuthServiceProvider,
      Provider<TraktTokenStore> traktTokenStoreProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    return new TraktSettingsViewModel_Factory(traktAuthServiceProvider, traktTokenStoreProvider, libraryRepositoryProvider);
  }

  public static TraktSettingsViewModel newInstance(TraktAuthService traktAuthService,
      TraktTokenStore traktTokenStore, LibraryRepository libraryRepository) {
    return new TraktSettingsViewModel(traktAuthService, traktTokenStore, libraryRepository);
  }
}
