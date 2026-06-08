package com.popcorntime.android.ui.player;

import android.content.Context;
import androidx.lifecycle.SavedStateHandle;
import com.popcorntime.android.data.cast.CastManager;
import com.popcorntime.android.data.cast.KodiPrefsStore;
import com.popcorntime.android.data.remote.PlaybackController;
import com.popcorntime.android.data.remote.PlaybackQueue;
import com.popcorntime.android.data.subtitles.SubtitleService;
import com.popcorntime.android.data.torrent.TorrentEngine;
import com.popcorntime.android.domain.repository.LibraryRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class PlayerViewModel_Factory implements Factory<PlayerViewModel> {
  private final Provider<TorrentEngine> torrentEngineProvider;

  private final Provider<SubtitleService> subtitleServiceProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  private final Provider<CastManager> castManagerProvider;

  private final Provider<KodiPrefsStore> kodiPrefsStoreProvider;

  private final Provider<PlaybackController> playbackControllerProvider;

  private final Provider<PlaybackQueue> playbackQueueProvider;

  private final Provider<Context> contextProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public PlayerViewModel_Factory(Provider<TorrentEngine> torrentEngineProvider,
      Provider<SubtitleService> subtitleServiceProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<CastManager> castManagerProvider, Provider<KodiPrefsStore> kodiPrefsStoreProvider,
      Provider<PlaybackController> playbackControllerProvider,
      Provider<PlaybackQueue> playbackQueueProvider, Provider<Context> contextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.torrentEngineProvider = torrentEngineProvider;
    this.subtitleServiceProvider = subtitleServiceProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
    this.castManagerProvider = castManagerProvider;
    this.kodiPrefsStoreProvider = kodiPrefsStoreProvider;
    this.playbackControllerProvider = playbackControllerProvider;
    this.playbackQueueProvider = playbackQueueProvider;
    this.contextProvider = contextProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public PlayerViewModel get() {
    return newInstance(torrentEngineProvider.get(), subtitleServiceProvider.get(), libraryRepositoryProvider.get(), castManagerProvider.get(), kodiPrefsStoreProvider.get(), playbackControllerProvider.get(), playbackQueueProvider.get(), contextProvider.get(), savedStateHandleProvider.get());
  }

  public static PlayerViewModel_Factory create(Provider<TorrentEngine> torrentEngineProvider,
      Provider<SubtitleService> subtitleServiceProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<CastManager> castManagerProvider, Provider<KodiPrefsStore> kodiPrefsStoreProvider,
      Provider<PlaybackController> playbackControllerProvider,
      Provider<PlaybackQueue> playbackQueueProvider, Provider<Context> contextProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new PlayerViewModel_Factory(torrentEngineProvider, subtitleServiceProvider, libraryRepositoryProvider, castManagerProvider, kodiPrefsStoreProvider, playbackControllerProvider, playbackQueueProvider, contextProvider, savedStateHandleProvider);
  }

  public static PlayerViewModel newInstance(TorrentEngine torrentEngine,
      SubtitleService subtitleService, LibraryRepository libraryRepository, CastManager castManager,
      KodiPrefsStore kodiPrefsStore, PlaybackController playbackController,
      PlaybackQueue playbackQueue, Context context, SavedStateHandle savedStateHandle) {
    return new PlayerViewModel(torrentEngine, subtitleService, libraryRepository, castManager, kodiPrefsStore, playbackController, playbackQueue, context, savedStateHandle);
  }
}
