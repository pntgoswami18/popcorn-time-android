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
public final class RemoteModule_ProvideRemoteDataStoreFactory implements Factory<DataStore<Preferences>> {
  private final Provider<Context> contextProvider;

  public RemoteModule_ProvideRemoteDataStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataStore<Preferences> get() {
    return provideRemoteDataStore(contextProvider.get());
  }

  public static RemoteModule_ProvideRemoteDataStoreFactory create(
      Provider<Context> contextProvider) {
    return new RemoteModule_ProvideRemoteDataStoreFactory(contextProvider);
  }

  public static DataStore<Preferences> provideRemoteDataStore(Context context) {
    return Preconditions.checkNotNullFromProvides(RemoteModule.INSTANCE.provideRemoteDataStore(context));
  }
}
