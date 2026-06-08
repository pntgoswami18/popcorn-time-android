package com.popcorntime.android.data.remote;

import com.popcorntime.android.data.torrent.TorrentEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class RemoteControlServer_Factory implements Factory<RemoteControlServer> {
  private final Provider<PlaybackController> playbackControllerProvider;

  private final Provider<PlaybackQueue> playbackQueueProvider;

  private final Provider<TorrentEngine> torrentEngineProvider;

  private final Provider<RemoteControlTokenStore> tokenStoreProvider;

  public RemoteControlServer_Factory(Provider<PlaybackController> playbackControllerProvider,
      Provider<PlaybackQueue> playbackQueueProvider, Provider<TorrentEngine> torrentEngineProvider,
      Provider<RemoteControlTokenStore> tokenStoreProvider) {
    this.playbackControllerProvider = playbackControllerProvider;
    this.playbackQueueProvider = playbackQueueProvider;
    this.torrentEngineProvider = torrentEngineProvider;
    this.tokenStoreProvider = tokenStoreProvider;
  }

  @Override
  public RemoteControlServer get() {
    return newInstance(playbackControllerProvider.get(), playbackQueueProvider.get(), torrentEngineProvider.get(), tokenStoreProvider.get());
  }

  public static RemoteControlServer_Factory create(
      Provider<PlaybackController> playbackControllerProvider,
      Provider<PlaybackQueue> playbackQueueProvider, Provider<TorrentEngine> torrentEngineProvider,
      Provider<RemoteControlTokenStore> tokenStoreProvider) {
    return new RemoteControlServer_Factory(playbackControllerProvider, playbackQueueProvider, torrentEngineProvider, tokenStoreProvider);
  }

  public static RemoteControlServer newInstance(PlaybackController playbackController,
      PlaybackQueue playbackQueue, TorrentEngine torrentEngine,
      RemoteControlTokenStore tokenStore) {
    return new RemoteControlServer(playbackController, playbackQueue, torrentEngine, tokenStore);
  }
}
