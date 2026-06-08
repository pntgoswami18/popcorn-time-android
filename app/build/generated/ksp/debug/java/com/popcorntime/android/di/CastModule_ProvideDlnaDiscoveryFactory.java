package com.popcorntime.android.di;

import android.content.Context;
import com.popcorntime.android.data.cast.DlnaDiscovery;
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
public final class CastModule_ProvideDlnaDiscoveryFactory implements Factory<DlnaDiscovery> {
  private final Provider<Context> contextProvider;

  public CastModule_ProvideDlnaDiscoveryFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DlnaDiscovery get() {
    return provideDlnaDiscovery(contextProvider.get());
  }

  public static CastModule_ProvideDlnaDiscoveryFactory create(Provider<Context> contextProvider) {
    return new CastModule_ProvideDlnaDiscoveryFactory(contextProvider);
  }

  public static DlnaDiscovery provideDlnaDiscovery(Context context) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideDlnaDiscovery(context));
  }
}
