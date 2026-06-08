package com.popcorntime.android.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.popcorntime.android.data.sources.TorrentSourcePrefs;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class SourceModule_ProvideTorrentSourcePrefsFactory implements Factory<TorrentSourcePrefs> {
  private final Provider<DataStore<Preferences>> dsProvider;

  public SourceModule_ProvideTorrentSourcePrefsFactory(
      Provider<DataStore<Preferences>> dsProvider) {
    this.dsProvider = dsProvider;
  }

  @Override
  public TorrentSourcePrefs get() {
    return provideTorrentSourcePrefs(dsProvider.get());
  }

  public static SourceModule_ProvideTorrentSourcePrefsFactory create(
      Provider<DataStore<Preferences>> dsProvider) {
    return new SourceModule_ProvideTorrentSourcePrefsFactory(dsProvider);
  }

  public static TorrentSourcePrefs provideTorrentSourcePrefs(DataStore<Preferences> ds) {
    return Preconditions.checkNotNullFromProvides(SourceModule.INSTANCE.provideTorrentSourcePrefs(ds));
  }
}
