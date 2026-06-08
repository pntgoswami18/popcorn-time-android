package com.popcorntime.android.di;

import android.content.Context;
import com.popcorntime.android.data.cast.CastManager;
import com.popcorntime.android.data.cast.ChromecastCaster;
import com.popcorntime.android.data.cast.DlnaCaster;
import com.popcorntime.android.data.cast.DlnaDiscovery;
import com.popcorntime.android.data.cast.KodiCaster;
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
public final class CastModule_ProvideCastManagerFactory implements Factory<CastManager> {
  private final Provider<Context> contextProvider;

  private final Provider<KodiCaster> kodiCasterProvider;

  private final Provider<DlnaCaster> dlnaCasterProvider;

  private final Provider<DlnaDiscovery> dlnaDiscoveryProvider;

  private final Provider<ChromecastCaster> chromeCasterProvider;

  public CastModule_ProvideCastManagerFactory(Provider<Context> contextProvider,
      Provider<KodiCaster> kodiCasterProvider, Provider<DlnaCaster> dlnaCasterProvider,
      Provider<DlnaDiscovery> dlnaDiscoveryProvider,
      Provider<ChromecastCaster> chromeCasterProvider) {
    this.contextProvider = contextProvider;
    this.kodiCasterProvider = kodiCasterProvider;
    this.dlnaCasterProvider = dlnaCasterProvider;
    this.dlnaDiscoveryProvider = dlnaDiscoveryProvider;
    this.chromeCasterProvider = chromeCasterProvider;
  }

  @Override
  public CastManager get() {
    return provideCastManager(contextProvider.get(), kodiCasterProvider.get(), dlnaCasterProvider.get(), dlnaDiscoveryProvider.get(), chromeCasterProvider.get());
  }

  public static CastModule_ProvideCastManagerFactory create(Provider<Context> contextProvider,
      Provider<KodiCaster> kodiCasterProvider, Provider<DlnaCaster> dlnaCasterProvider,
      Provider<DlnaDiscovery> dlnaDiscoveryProvider,
      Provider<ChromecastCaster> chromeCasterProvider) {
    return new CastModule_ProvideCastManagerFactory(contextProvider, kodiCasterProvider, dlnaCasterProvider, dlnaDiscoveryProvider, chromeCasterProvider);
  }

  public static CastManager provideCastManager(Context context, KodiCaster kodiCaster,
      DlnaCaster dlnaCaster, DlnaDiscovery dlnaDiscovery, ChromecastCaster chromeCaster) {
    return Preconditions.checkNotNullFromProvides(CastModule.INSTANCE.provideCastManager(context, kodiCaster, dlnaCaster, dlnaDiscovery, chromeCaster));
  }
}
