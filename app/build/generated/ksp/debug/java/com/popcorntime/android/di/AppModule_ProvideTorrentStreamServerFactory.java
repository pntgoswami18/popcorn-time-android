package com.popcorntime.android.di;

import com.popcorntime.android.data.torrent.TorrentStreamServer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AppModule_ProvideTorrentStreamServerFactory implements Factory<TorrentStreamServer> {
  @Override
  public TorrentStreamServer get() {
    return provideTorrentStreamServer();
  }

  public static AppModule_ProvideTorrentStreamServerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TorrentStreamServer provideTorrentStreamServer() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTorrentStreamServer());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideTorrentStreamServerFactory INSTANCE = new AppModule_ProvideTorrentStreamServerFactory();
  }
}
