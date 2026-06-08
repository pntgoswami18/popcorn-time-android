package com.popcorntime.android.data.torrent;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.io.File;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TorrentEngine_Factory implements Factory<TorrentEngine> {
  private final Provider<File> cacheDirProvider;

  private final Provider<TorrentStreamServer> streamServerProvider;

  public TorrentEngine_Factory(Provider<File> cacheDirProvider,
      Provider<TorrentStreamServer> streamServerProvider) {
    this.cacheDirProvider = cacheDirProvider;
    this.streamServerProvider = streamServerProvider;
  }

  @Override
  public TorrentEngine get() {
    return newInstance(cacheDirProvider.get(), streamServerProvider.get());
  }

  public static TorrentEngine_Factory create(Provider<File> cacheDirProvider,
      Provider<TorrentStreamServer> streamServerProvider) {
    return new TorrentEngine_Factory(cacheDirProvider, streamServerProvider);
  }

  public static TorrentEngine newInstance(File cacheDir, TorrentStreamServer streamServer) {
    return new TorrentEngine(cacheDir, streamServer);
  }
}
