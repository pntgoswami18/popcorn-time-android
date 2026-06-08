package com.popcorntime.android.di;

import android.content.Context;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata({
    "javax.inject.Named",
    "dagger.hilt.android.qualifiers.ApplicationContext"
})
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
public final class SubtitleModule_ProvideOsDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  public SubtitleModule_ProvideOsDataStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideOsDataStore(contextProvider.get());
  }

  public static SubtitleModule_ProvideOsDataStoreFactory create(Provider<Context> contextProvider) {
    return new SubtitleModule_ProvideOsDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideOsDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(SubtitleModule.INSTANCE.provideOsDataStore(context));
  }
}
