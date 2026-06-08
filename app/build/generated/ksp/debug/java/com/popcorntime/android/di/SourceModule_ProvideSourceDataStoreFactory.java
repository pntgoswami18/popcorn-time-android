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
public final class SourceModule_ProvideSourceDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> ctxProvider;

  public SourceModule_ProvideSourceDataStoreFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideSourceDataStore(ctxProvider.get());
  }

  public static SourceModule_ProvideSourceDataStoreFactory create(Provider<Context> ctxProvider) {
    return new SourceModule_ProvideSourceDataStoreFactory(ctxProvider);
  }

  public static DataStore<Preferences> provideSourceDataStore(Context ctx) {
    return Preconditions.checkNotNullFromProvides(SourceModule.INSTANCE.provideSourceDataStore(ctx));
  }
}
