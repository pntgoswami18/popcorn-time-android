package com.popcorntime.android.di;

import android.content.Context;
import com.popcorntime.android.data.cast.ChromecastCaster;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class CastModule_ProvideChromecastCasterFactory implements Factory<ChromecastCaster> {
  private final Provider<Context> contextProvider;

  public CastModule_ProvideChromecastCasterFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ChromecastCaster get() {
    return provideChromecastCaster(contextProvider.get());
  }

  public static CastModule_ProvideChromecastCasterFactory create(
      Provider<Context> contextProvider) {
    return new CastModule_ProvideChromecastCasterFactory(contextProvider);
  }

  public static ChromecastCaster provideChromecastCaster(Context context) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideChromecastCaster(context));
  }
}
