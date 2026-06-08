package com.popcorntime.android.ui.settings;

import com.popcorntime.android.data.sources.TorrentSourcePrefs;
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
public final class SourceSettingsViewModel_Factory implements Factory<SourceSettingsViewModel> {
  private final Provider<TorrentSourcePrefs> sourcePrefsProvider;

  public SourceSettingsViewModel_Factory(Provider<TorrentSourcePrefs> sourcePrefsProvider) {
    this.sourcePrefsProvider = sourcePrefsProvider;
  }

  @Override
  public SourceSettingsViewModel get() {
    return newInstance(sourcePrefsProvider.get());
  }

  public static SourceSettingsViewModel_Factory create(
      Provider<TorrentSourcePrefs> sourcePrefsProvider) {
    return new SourceSettingsViewModel_Factory(sourcePrefsProvider);
  }

  public static SourceSettingsViewModel newInstance(TorrentSourcePrefs sourcePrefs) {
    return new SourceSettingsViewModel(sourcePrefs);
  }
}
