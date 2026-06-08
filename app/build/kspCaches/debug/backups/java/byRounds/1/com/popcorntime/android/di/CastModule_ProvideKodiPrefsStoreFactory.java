package com.popcorntime.android.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.popcorntime.android.data.cast.KodiPrefsStore;
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
public final class CastModule_ProvideKodiPrefsStoreFactory implements Factory<KodiPrefsStore> {
  private final Provider<DataStore<Preferences>> dsProvider;

  public CastModule_ProvideKodiPrefsStoreFactory(Provider<DataStore<Preferences>> dsProvider) {
    this.dsProvider = dsProvider;
  }

  @Override
  public KodiPrefsStore get() {
    return provideKodiPrefsStore(dsProvider.get());
  }

  public static CastModule_ProvideKodiPrefsStoreFactory create(
      Provider<DataStore<Preferences>> dsProvider) {
    return new CastModule_ProvideKodiPrefsStoreFactory(dsProvider);
  }

  public static KodiPrefsStore provideKodiPrefsStore(DataStore<Preferences> ds) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideKodiPrefsStore(ds));
  }
}
