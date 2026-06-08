package com.popcorntime.android.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.List;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideMovieServersFactory implements Factory<List<String>> {
  @Override
  public List<String> get() {
    return provideMovieServers();
  }

  public static AppModule_ProvideMovieServersFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static List<String> provideMovieServers() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMovieServers());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideMovieServersFactory INSTANCE = new AppModule_ProvideMovieServersFactory();
  }
}
