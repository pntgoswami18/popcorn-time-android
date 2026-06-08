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
public final class CastModule_ProvideCastDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  public CastModule_ProvideCastDataStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideCastDataStore(contextProvider.get());
  }

  public static CastModule_ProvideCastDataStoreFactory create(Provider<Context> contextProvider) {
    return new CastModule_ProvideCastDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideCastDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideCastDataStore(context));
  }
}
