package com.popcorntime.android.data.torrent;

import com.popcorntime.android.data.remote.RemoteControlServer;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class TorrentService_MembersInjector implements MembersInjector<TorrentService> {
  private final Provider<TorrentEngine> torrentEngineProvider;

  private final Provider<RemoteControlServer> remoteControlServerProvider;

  public TorrentService_MembersInjector(Provider<TorrentEngine> torrentEngineProvider,
      Provider<RemoteControlServer> remoteControlServerProvider) {
    this.torrentEngineProvider = torrentEngineProvider;
    this.remoteControlServerProvider = remoteControlServerProvider;
  }

  public static MembersInjector<TorrentService> create(
      Provider<TorrentEngine> torrentEngineProvider,
      Provider<RemoteControlServer> remoteControlServerProvider) {
    return new TorrentService_MembersInjector(torrentEngineProvider, remoteControlServerProvider);
  }

  @Override
  public void injectMembers(TorrentService instance) {
    injectTorrentEngine(instance, torrentEngineProvider.get());
    injectRemoteControlServer(instance, remoteControlServerProvider.get());
  }

  @InjectedFieldSignature("com.popcorntime.android.data.torrent.TorrentService.torrentEngine")
  public static void injectTorrentEngine(TorrentService instance, TorrentEngine torrentEngine) {
    instance.torrentEngine = torrentEngine;
  }

  @InjectedFieldSignature("com.popcorntime.android.data.torrent.TorrentService.remoteControlServer")
  public static void injectRemoteControlServer(TorrentService instance,
      RemoteControlServer remoteControlServer) {
    instance.remoteControlServer = remoteControlServer;
  }
}
