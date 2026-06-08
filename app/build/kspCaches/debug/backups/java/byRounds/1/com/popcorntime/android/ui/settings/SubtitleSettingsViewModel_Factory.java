package com.popcorntime.android.ui.settings;

import com.popcorntime.android.data.subtitles.OsAuthService;
import com.popcorntime.android.data.subtitles.OsTokenStore;
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
public final class SubtitleSettingsViewModel_Factory implements Factory<SubtitleSettingsViewModel> {
  private final Provider<OsAuthService> osAuthServiceProvider;

  private final Provider<OsTokenStore> osTokenStoreProvider;

  public SubtitleSettingsViewModel_Factory(Provider<OsAuthService> osAuthServiceProvider,
      Provider<OsTokenStore> osTokenStoreProvider) {
    this.osAuthServiceProvider = osAuthServiceProvider;
    this.osTokenStoreProvider = osTokenStoreProvider;
  }

  @Override
  public SubtitleSettingsViewModel get() {
    return newInstance(osAuthServiceProvider.get(), osTokenStoreProvider.get());
  }

  public static SubtitleSettingsViewModel_Factory create(
      Provider<OsAuthService> osAuthServiceProvider, Provider<OsTokenStore> osTokenStoreProvider) {
    return new SubtitleSettingsViewModel_Factory(osAuthServiceProvider, osTokenStoreProvider);
  }

  public static SubtitleSettingsViewModel newInstance(OsAuthService osAuthService,
      OsTokenStore osTokenStore) {
    return new SubtitleSettingsViewModel(osAuthService, osTokenStore);
  }
}
