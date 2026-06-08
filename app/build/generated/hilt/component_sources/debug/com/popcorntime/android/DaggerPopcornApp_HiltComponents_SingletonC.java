package com.popcorntime.android;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.popcorntime.android.data.api.JackettApiService;
import com.popcorntime.android.data.api.MovieApiService;
import com.popcorntime.android.data.api.ShowApiService;
import com.popcorntime.android.data.cast.CastManager;
import com.popcorntime.android.data.cast.ChromecastCaster;
import com.popcorntime.android.data.cast.DlnaCaster;
import com.popcorntime.android.data.cast.DlnaDiscovery;
import com.popcorntime.android.data.cast.KodiCaster;
import com.popcorntime.android.data.cast.KodiPrefsStore;
import com.popcorntime.android.data.db.AppDatabase;
import com.popcorntime.android.data.db.dao.BookmarkedDao;
import com.popcorntime.android.data.db.dao.LibraryItemDao;
import com.popcorntime.android.data.db.dao.WatchedDao;
import com.popcorntime.android.data.db.dao.WatchlistDao;
import com.popcorntime.android.data.remote.PlaybackController;
import com.popcorntime.android.data.remote.PlaybackQueue;
import com.popcorntime.android.data.remote.RemoteControlServer;
import com.popcorntime.android.data.remote.RemoteControlTokenStore;
import com.popcorntime.android.data.repository.LibraryRepositoryImpl;
import com.popcorntime.android.data.repository.MovieRepositoryImpl;
import com.popcorntime.android.data.repository.ShowRepositoryImpl;
import com.popcorntime.android.data.sources.TorrentSourcePrefs;
import com.popcorntime.android.data.subtitles.OsAuthService;
import com.popcorntime.android.data.subtitles.OsTokenStore;
import com.popcorntime.android.data.subtitles.SubtitleService;
import com.popcorntime.android.data.torrent.TorrentEngine;
import com.popcorntime.android.data.torrent.TorrentService;
import com.popcorntime.android.data.torrent.TorrentService_MembersInjector;
import com.popcorntime.android.data.torrent.TorrentStreamServer;
import com.popcorntime.android.data.trakt.TraktAuthService;
import com.popcorntime.android.data.trakt.TraktSyncService;
import com.popcorntime.android.data.trakt.TraktTokenStore;
import com.popcorntime.android.di.AppModule_ProvideMovieServersFactory;
import com.popcorntime.android.di.AppModule_ProvideTorrentCacheDirFactory;
import com.popcorntime.android.di.AppModule_ProvideTorrentStreamServerFactory;
import com.popcorntime.android.di.CastModule_ProvideCastDataStoreFactory;
import com.popcorntime.android.di.CastModule_ProvideCastManagerFactory;
import com.popcorntime.android.di.CastModule_ProvideChromecastCasterFactory;
import com.popcorntime.android.di.CastModule_ProvideDlnaCasterFactory;
import com.popcorntime.android.di.CastModule_ProvideDlnaDiscoveryFactory;
import com.popcorntime.android.di.CastModule_ProvideKodiCasterFactory;
import com.popcorntime.android.di.CastModule_ProvideKodiPrefsStoreFactory;
import com.popcorntime.android.di.DatabaseModule_ProvideBookmarkedDaoFactory;
import com.popcorntime.android.di.DatabaseModule_ProvideDatabaseFactory;
import com.popcorntime.android.di.DatabaseModule_ProvideLibraryItemDaoFactory;
import com.popcorntime.android.di.DatabaseModule_ProvideWatchedDaoFactory;
import com.popcorntime.android.di.DatabaseModule_ProvideWatchlistDaoFactory;
import com.popcorntime.android.di.NetworkModule_ProvideHttpClientFactory;
import com.popcorntime.android.di.NetworkModule_ProvideJsonFactory;
import com.popcorntime.android.di.RemoteModule_ProvideRemoteControlTokenStoreFactory;
import com.popcorntime.android.di.RemoteModule_ProvideRemoteDataStoreFactory;
import com.popcorntime.android.di.SourceModule_ProvideJackettApiServiceFactory;
import com.popcorntime.android.di.SourceModule_ProvideSourceDataStoreFactory;
import com.popcorntime.android.di.SourceModule_ProvideTorrentSourcePrefsFactory;
import com.popcorntime.android.di.SubtitleModule_ProvideOsAuthServiceFactory;
import com.popcorntime.android.di.SubtitleModule_ProvideOsDataStoreFactory;
import com.popcorntime.android.di.SubtitleModule_ProvideOsTokenStoreFactory;
import com.popcorntime.android.di.SubtitleModule_ProvideSubtitleServiceFactory;
import com.popcorntime.android.di.TraktModule_ProvideDataStoreFactory;
import com.popcorntime.android.di.TraktModule_ProvideTraktAuthServiceFactory;
import com.popcorntime.android.di.TraktModule_ProvideTraktHttpClientFactory;
import com.popcorntime.android.di.TraktModule_ProvideTraktSyncServiceFactory;
import com.popcorntime.android.di.TraktModule_ProvideTraktTokenStoreFactory;
import com.popcorntime.android.ui.library.LibraryViewModel;
import com.popcorntime.android.ui.library.LibraryViewModel_HiltModules;
import com.popcorntime.android.ui.movies.MovieBrowserViewModel;
import com.popcorntime.android.ui.movies.MovieBrowserViewModel_HiltModules;
import com.popcorntime.android.ui.movies.MovieDetailViewModel;
import com.popcorntime.android.ui.movies.MovieDetailViewModel_HiltModules;
import com.popcorntime.android.ui.player.PlayerViewModel;
import com.popcorntime.android.ui.player.PlayerViewModel_HiltModules;
import com.popcorntime.android.ui.settings.RemoteSettingsViewModel;
import com.popcorntime.android.ui.settings.RemoteSettingsViewModel_HiltModules;
import com.popcorntime.android.ui.settings.SourceSettingsViewModel;
import com.popcorntime.android.ui.settings.SourceSettingsViewModel_HiltModules;
import com.popcorntime.android.ui.settings.SubtitleSettingsViewModel;
import com.popcorntime.android.ui.settings.SubtitleSettingsViewModel_HiltModules;
import com.popcorntime.android.ui.settings.TraktSettingsViewModel;
import com.popcorntime.android.ui.settings.TraktSettingsViewModel_HiltModules;
import com.popcorntime.android.ui.shows.ShowBrowserViewModel;
import com.popcorntime.android.ui.shows.ShowBrowserViewModel_HiltModules;
import com.popcorntime.android.ui.shows.ShowDetailViewModel;
import com.popcorntime.android.ui.shows.ShowDetailViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.ktor.client.HttpClient;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;

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
public final class DaggerPopcornApp_HiltComponents_SingletonC {
  private DaggerPopcornApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public PopcornApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements PopcornApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements PopcornApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements PopcornApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements PopcornApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements PopcornApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements PopcornApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements PopcornApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public PopcornApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends PopcornApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends PopcornApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends PopcornApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends PopcornApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_popcorntime_android_ui_library_LibraryViewModel, LibraryViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_movies_MovieBrowserViewModel, MovieBrowserViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_movies_MovieDetailViewModel, MovieDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_player_PlayerViewModel, PlayerViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_RemoteSettingsViewModel, RemoteSettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_shows_ShowBrowserViewModel, ShowBrowserViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_shows_ShowDetailViewModel, ShowDetailViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_SourceSettingsViewModel, SourceSettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_SubtitleSettingsViewModel, SubtitleSettingsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_TraktSettingsViewModel, TraktSettingsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_popcorntime_android_ui_settings_TraktSettingsViewModel = "com.popcorntime.android.ui.settings.TraktSettingsViewModel";

      static String com_popcorntime_android_ui_settings_SourceSettingsViewModel = "com.popcorntime.android.ui.settings.SourceSettingsViewModel";

      static String com_popcorntime_android_ui_movies_MovieDetailViewModel = "com.popcorntime.android.ui.movies.MovieDetailViewModel";

      static String com_popcorntime_android_ui_player_PlayerViewModel = "com.popcorntime.android.ui.player.PlayerViewModel";

      static String com_popcorntime_android_ui_shows_ShowBrowserViewModel = "com.popcorntime.android.ui.shows.ShowBrowserViewModel";

      static String com_popcorntime_android_ui_library_LibraryViewModel = "com.popcorntime.android.ui.library.LibraryViewModel";

      static String com_popcorntime_android_ui_shows_ShowDetailViewModel = "com.popcorntime.android.ui.shows.ShowDetailViewModel";

      static String com_popcorntime_android_ui_settings_RemoteSettingsViewModel = "com.popcorntime.android.ui.settings.RemoteSettingsViewModel";

      static String com_popcorntime_android_ui_movies_MovieBrowserViewModel = "com.popcorntime.android.ui.movies.MovieBrowserViewModel";

      static String com_popcorntime_android_ui_settings_SubtitleSettingsViewModel = "com.popcorntime.android.ui.settings.SubtitleSettingsViewModel";

      @KeepFieldType
      TraktSettingsViewModel com_popcorntime_android_ui_settings_TraktSettingsViewModel2;

      @KeepFieldType
      SourceSettingsViewModel com_popcorntime_android_ui_settings_SourceSettingsViewModel2;

      @KeepFieldType
      MovieDetailViewModel com_popcorntime_android_ui_movies_MovieDetailViewModel2;

      @KeepFieldType
      PlayerViewModel com_popcorntime_android_ui_player_PlayerViewModel2;

      @KeepFieldType
      ShowBrowserViewModel com_popcorntime_android_ui_shows_ShowBrowserViewModel2;

      @KeepFieldType
      LibraryViewModel com_popcorntime_android_ui_library_LibraryViewModel2;

      @KeepFieldType
      ShowDetailViewModel com_popcorntime_android_ui_shows_ShowDetailViewModel2;

      @KeepFieldType
      RemoteSettingsViewModel com_popcorntime_android_ui_settings_RemoteSettingsViewModel2;

      @KeepFieldType
      MovieBrowserViewModel com_popcorntime_android_ui_movies_MovieBrowserViewModel2;

      @KeepFieldType
      SubtitleSettingsViewModel com_popcorntime_android_ui_settings_SubtitleSettingsViewModel2;
    }
  }

  private static final class ViewModelCImpl extends PopcornApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<LibraryViewModel> libraryViewModelProvider;

    private Provider<MovieBrowserViewModel> movieBrowserViewModelProvider;

    private Provider<MovieDetailViewModel> movieDetailViewModelProvider;

    private Provider<PlayerViewModel> playerViewModelProvider;

    private Provider<RemoteSettingsViewModel> remoteSettingsViewModelProvider;

    private Provider<ShowBrowserViewModel> showBrowserViewModelProvider;

    private Provider<ShowDetailViewModel> showDetailViewModelProvider;

    private Provider<SourceSettingsViewModel> sourceSettingsViewModelProvider;

    private Provider<SubtitleSettingsViewModel> subtitleSettingsViewModelProvider;

    private Provider<TraktSettingsViewModel> traktSettingsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.libraryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.movieBrowserViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.movieDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.playerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.remoteSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.showBrowserViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.showDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.sourceSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.subtitleSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.traktSettingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(10).put(LazyClassKeyProvider.com_popcorntime_android_ui_library_LibraryViewModel, ((Provider) libraryViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_movies_MovieBrowserViewModel, ((Provider) movieBrowserViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_movies_MovieDetailViewModel, ((Provider) movieDetailViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_player_PlayerViewModel, ((Provider) playerViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_RemoteSettingsViewModel, ((Provider) remoteSettingsViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_shows_ShowBrowserViewModel, ((Provider) showBrowserViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_shows_ShowDetailViewModel, ((Provider) showDetailViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_SourceSettingsViewModel, ((Provider) sourceSettingsViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_SubtitleSettingsViewModel, ((Provider) subtitleSettingsViewModelProvider)).put(LazyClassKeyProvider.com_popcorntime_android_ui_settings_TraktSettingsViewModel, ((Provider) traktSettingsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_popcorntime_android_ui_player_PlayerViewModel = "com.popcorntime.android.ui.player.PlayerViewModel";

      static String com_popcorntime_android_ui_settings_SourceSettingsViewModel = "com.popcorntime.android.ui.settings.SourceSettingsViewModel";

      static String com_popcorntime_android_ui_movies_MovieBrowserViewModel = "com.popcorntime.android.ui.movies.MovieBrowserViewModel";

      static String com_popcorntime_android_ui_library_LibraryViewModel = "com.popcorntime.android.ui.library.LibraryViewModel";

      static String com_popcorntime_android_ui_settings_RemoteSettingsViewModel = "com.popcorntime.android.ui.settings.RemoteSettingsViewModel";

      static String com_popcorntime_android_ui_movies_MovieDetailViewModel = "com.popcorntime.android.ui.movies.MovieDetailViewModel";

      static String com_popcorntime_android_ui_shows_ShowDetailViewModel = "com.popcorntime.android.ui.shows.ShowDetailViewModel";

      static String com_popcorntime_android_ui_shows_ShowBrowserViewModel = "com.popcorntime.android.ui.shows.ShowBrowserViewModel";

      static String com_popcorntime_android_ui_settings_SubtitleSettingsViewModel = "com.popcorntime.android.ui.settings.SubtitleSettingsViewModel";

      static String com_popcorntime_android_ui_settings_TraktSettingsViewModel = "com.popcorntime.android.ui.settings.TraktSettingsViewModel";

      @KeepFieldType
      PlayerViewModel com_popcorntime_android_ui_player_PlayerViewModel2;

      @KeepFieldType
      SourceSettingsViewModel com_popcorntime_android_ui_settings_SourceSettingsViewModel2;

      @KeepFieldType
      MovieBrowserViewModel com_popcorntime_android_ui_movies_MovieBrowserViewModel2;

      @KeepFieldType
      LibraryViewModel com_popcorntime_android_ui_library_LibraryViewModel2;

      @KeepFieldType
      RemoteSettingsViewModel com_popcorntime_android_ui_settings_RemoteSettingsViewModel2;

      @KeepFieldType
      MovieDetailViewModel com_popcorntime_android_ui_movies_MovieDetailViewModel2;

      @KeepFieldType
      ShowDetailViewModel com_popcorntime_android_ui_shows_ShowDetailViewModel2;

      @KeepFieldType
      ShowBrowserViewModel com_popcorntime_android_ui_shows_ShowBrowserViewModel2;

      @KeepFieldType
      SubtitleSettingsViewModel com_popcorntime_android_ui_settings_SubtitleSettingsViewModel2;

      @KeepFieldType
      TraktSettingsViewModel com_popcorntime_android_ui_settings_TraktSettingsViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.popcorntime.android.ui.library.LibraryViewModel 
          return (T) new LibraryViewModel(singletonCImpl.libraryRepositoryImplProvider.get());

          case 1: // com.popcorntime.android.ui.movies.MovieBrowserViewModel 
          return (T) new MovieBrowserViewModel(singletonCImpl.movieRepositoryImplProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get());

          case 2: // com.popcorntime.android.ui.movies.MovieDetailViewModel 
          return (T) new MovieDetailViewModel(singletonCImpl.movieRepositoryImplProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.popcorntime.android.ui.player.PlayerViewModel 
          return (T) new PlayerViewModel(singletonCImpl.torrentEngineProvider.get(), singletonCImpl.provideSubtitleServiceProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get(), singletonCImpl.provideCastManagerProvider.get(), singletonCImpl.provideKodiPrefsStoreProvider.get(), singletonCImpl.playbackControllerProvider.get(), singletonCImpl.playbackQueueProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), viewModelCImpl.savedStateHandle);

          case 4: // com.popcorntime.android.ui.settings.RemoteSettingsViewModel 
          return (T) new RemoteSettingsViewModel(singletonCImpl.remoteControlServerProvider.get(), singletonCImpl.provideRemoteControlTokenStoreProvider.get());

          case 5: // com.popcorntime.android.ui.shows.ShowBrowserViewModel 
          return (T) new ShowBrowserViewModel(singletonCImpl.showRepositoryImplProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get());

          case 6: // com.popcorntime.android.ui.shows.ShowDetailViewModel 
          return (T) new ShowDetailViewModel(singletonCImpl.showRepositoryImplProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get(), viewModelCImpl.savedStateHandle);

          case 7: // com.popcorntime.android.ui.settings.SourceSettingsViewModel 
          return (T) new SourceSettingsViewModel(singletonCImpl.provideTorrentSourcePrefsProvider.get());

          case 8: // com.popcorntime.android.ui.settings.SubtitleSettingsViewModel 
          return (T) new SubtitleSettingsViewModel(singletonCImpl.provideOsAuthServiceProvider.get(), singletonCImpl.provideOsTokenStoreProvider.get());

          case 9: // com.popcorntime.android.ui.settings.TraktSettingsViewModel 
          return (T) new TraktSettingsViewModel(singletonCImpl.provideTraktAuthServiceProvider.get(), singletonCImpl.provideTraktTokenStoreProvider.get(), singletonCImpl.libraryRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends PopcornApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends PopcornApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectTorrentService(TorrentService torrentService) {
      injectTorrentService2(torrentService);
    }

    private TorrentService injectTorrentService2(TorrentService instance) {
      TorrentService_MembersInjector.injectTorrentEngine(instance, singletonCImpl.torrentEngineProvider.get());
      TorrentService_MembersInjector.injectRemoteControlServer(instance, singletonCImpl.remoteControlServerProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends PopcornApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<HttpClient> provideTraktHttpClientProvider;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<TraktTokenStore> provideTraktTokenStoreProvider;

    private Provider<TraktSyncService> provideTraktSyncServiceProvider;

    private Provider<LibraryRepositoryImpl> libraryRepositoryImplProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<HttpClient> provideHttpClientProvider;

    private Provider<List<String>> provideMovieServersProvider;

    private Provider<MovieApiService> movieApiServiceProvider;

    private Provider<DataStore<Preferences>> provideSourceDataStoreProvider;

    private Provider<TorrentSourcePrefs> provideTorrentSourcePrefsProvider;

    private Provider<JackettApiService> provideJackettApiServiceProvider;

    private Provider<MovieRepositoryImpl> movieRepositoryImplProvider;

    private Provider<File> provideTorrentCacheDirProvider;

    private Provider<TorrentStreamServer> provideTorrentStreamServerProvider;

    private Provider<TorrentEngine> torrentEngineProvider;

    private Provider<DataStore<Preferences>> provideOsDataStoreProvider;

    private Provider<OsTokenStore> provideOsTokenStoreProvider;

    private Provider<SubtitleService> provideSubtitleServiceProvider;

    private Provider<KodiCaster> provideKodiCasterProvider;

    private Provider<DlnaCaster> provideDlnaCasterProvider;

    private Provider<DlnaDiscovery> provideDlnaDiscoveryProvider;

    private Provider<ChromecastCaster> provideChromecastCasterProvider;

    private Provider<CastManager> provideCastManagerProvider;

    private Provider<DataStore<Preferences>> provideCastDataStoreProvider;

    private Provider<KodiPrefsStore> provideKodiPrefsStoreProvider;

    private Provider<PlaybackController> playbackControllerProvider;

    private Provider<PlaybackQueue> playbackQueueProvider;

    private Provider<DataStore<Preferences>> provideRemoteDataStoreProvider;

    private Provider<RemoteControlTokenStore> provideRemoteControlTokenStoreProvider;

    private Provider<RemoteControlServer> remoteControlServerProvider;

    private Provider<ShowApiService> showApiServiceProvider;

    private Provider<ShowRepositoryImpl> showRepositoryImplProvider;

    private Provider<OsAuthService> provideOsAuthServiceProvider;

    private Provider<TraktAuthService> provideTraktAuthServiceProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private BookmarkedDao bookmarkedDao() {
      return DatabaseModule_ProvideBookmarkedDaoFactory.provideBookmarkedDao(provideDatabaseProvider.get());
    }

    private WatchedDao watchedDao() {
      return DatabaseModule_ProvideWatchedDaoFactory.provideWatchedDao(provideDatabaseProvider.get());
    }

    private WatchlistDao watchlistDao() {
      return DatabaseModule_ProvideWatchlistDaoFactory.provideWatchlistDao(provideDatabaseProvider.get());
    }

    private LibraryItemDao libraryItemDao() {
      return DatabaseModule_ProvideLibraryItemDaoFactory.provideLibraryItemDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 1));
      this.provideTraktHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<HttpClient>(singletonCImpl, 3));
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 5));
      this.provideTraktTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<TraktTokenStore>(singletonCImpl, 4));
      this.provideTraktSyncServiceProvider = DoubleCheck.provider(new SwitchingProvider<TraktSyncService>(singletonCImpl, 2));
      this.libraryRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<LibraryRepositoryImpl>(singletonCImpl, 0));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 9));
      this.provideHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<HttpClient>(singletonCImpl, 8));
      this.provideMovieServersProvider = DoubleCheck.provider(new SwitchingProvider<List<String>>(singletonCImpl, 10));
      this.movieApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<MovieApiService>(singletonCImpl, 7));
      this.provideSourceDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 12));
      this.provideTorrentSourcePrefsProvider = DoubleCheck.provider(new SwitchingProvider<TorrentSourcePrefs>(singletonCImpl, 11));
      this.provideJackettApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<JackettApiService>(singletonCImpl, 13));
      this.movieRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<MovieRepositoryImpl>(singletonCImpl, 6));
      this.provideTorrentCacheDirProvider = DoubleCheck.provider(new SwitchingProvider<File>(singletonCImpl, 15));
      this.provideTorrentStreamServerProvider = DoubleCheck.provider(new SwitchingProvider<TorrentStreamServer>(singletonCImpl, 16));
      this.torrentEngineProvider = DoubleCheck.provider(new SwitchingProvider<TorrentEngine>(singletonCImpl, 14));
      this.provideOsDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 19));
      this.provideOsTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<OsTokenStore>(singletonCImpl, 18));
      this.provideSubtitleServiceProvider = DoubleCheck.provider(new SwitchingProvider<SubtitleService>(singletonCImpl, 17));
      this.provideKodiCasterProvider = DoubleCheck.provider(new SwitchingProvider<KodiCaster>(singletonCImpl, 21));
      this.provideDlnaCasterProvider = DoubleCheck.provider(new SwitchingProvider<DlnaCaster>(singletonCImpl, 22));
      this.provideDlnaDiscoveryProvider = DoubleCheck.provider(new SwitchingProvider<DlnaDiscovery>(singletonCImpl, 23));
      this.provideChromecastCasterProvider = DoubleCheck.provider(new SwitchingProvider<ChromecastCaster>(singletonCImpl, 24));
      this.provideCastManagerProvider = DoubleCheck.provider(new SwitchingProvider<CastManager>(singletonCImpl, 20));
      this.provideCastDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 26));
      this.provideKodiPrefsStoreProvider = DoubleCheck.provider(new SwitchingProvider<KodiPrefsStore>(singletonCImpl, 25));
      this.playbackControllerProvider = DoubleCheck.provider(new SwitchingProvider<PlaybackController>(singletonCImpl, 27));
      this.playbackQueueProvider = DoubleCheck.provider(new SwitchingProvider<PlaybackQueue>(singletonCImpl, 28));
      this.provideRemoteDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 31));
      this.provideRemoteControlTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<RemoteControlTokenStore>(singletonCImpl, 30));
      this.remoteControlServerProvider = DoubleCheck.provider(new SwitchingProvider<RemoteControlServer>(singletonCImpl, 29));
      this.showApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<ShowApiService>(singletonCImpl, 33));
      this.showRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ShowRepositoryImpl>(singletonCImpl, 32));
      this.provideOsAuthServiceProvider = DoubleCheck.provider(new SwitchingProvider<OsAuthService>(singletonCImpl, 34));
      this.provideTraktAuthServiceProvider = DoubleCheck.provider(new SwitchingProvider<TraktAuthService>(singletonCImpl, 35));
    }

    @Override
    public void injectPopcornApp(PopcornApp popcornApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.popcorntime.android.data.repository.LibraryRepositoryImpl 
          return (T) new LibraryRepositoryImpl(singletonCImpl.bookmarkedDao(), singletonCImpl.watchedDao(), singletonCImpl.watchlistDao(), singletonCImpl.libraryItemDao(), singletonCImpl.provideTraktSyncServiceProvider.get(), singletonCImpl.provideTraktTokenStoreProvider.get());

          case 1: // com.popcorntime.android.data.db.AppDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.popcorntime.android.data.trakt.TraktSyncService 
          return (T) TraktModule_ProvideTraktSyncServiceFactory.provideTraktSyncService(singletonCImpl.provideTraktHttpClientProvider.get(), singletonCImpl.provideTraktTokenStoreProvider.get());

          case 3: // @javax.inject.Named("trakt") io.ktor.client.HttpClient 
          return (T) TraktModule_ProvideTraktHttpClientFactory.provideTraktHttpClient();

          case 4: // com.popcorntime.android.data.trakt.TraktTokenStore 
          return (T) TraktModule_ProvideTraktTokenStoreFactory.provideTraktTokenStore(singletonCImpl.provideDataStoreProvider.get());

          case 5: // @javax.inject.Named("traktDataStore") androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) TraktModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.popcorntime.android.data.repository.MovieRepositoryImpl 
          return (T) new MovieRepositoryImpl(singletonCImpl.movieApiServiceProvider.get(), singletonCImpl.watchedDao(), singletonCImpl.bookmarkedDao(), singletonCImpl.provideTorrentSourcePrefsProvider.get(), singletonCImpl.provideJackettApiServiceProvider.get());

          case 7: // com.popcorntime.android.data.api.MovieApiService 
          return (T) new MovieApiService(singletonCImpl.provideHttpClientProvider.get(), singletonCImpl.provideMovieServersProvider.get());

          case 8: // io.ktor.client.HttpClient 
          return (T) NetworkModule_ProvideHttpClientFactory.provideHttpClient(singletonCImpl.provideJsonProvider.get());

          case 9: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 10: // @javax.inject.Named("movieServers") java.util.List<java.lang.String> 
          return (T) AppModule_ProvideMovieServersFactory.provideMovieServers();

          case 11: // com.popcorntime.android.data.sources.TorrentSourcePrefs 
          return (T) SourceModule_ProvideTorrentSourcePrefsFactory.provideTorrentSourcePrefs(singletonCImpl.provideSourceDataStoreProvider.get());

          case 12: // @javax.inject.Named("sourceDataStore") androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) SourceModule_ProvideSourceDataStoreFactory.provideSourceDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 13: // com.popcorntime.android.data.api.JackettApiService 
          return (T) SourceModule_ProvideJackettApiServiceFactory.provideJackettApiService(singletonCImpl.provideHttpClientProvider.get());

          case 14: // com.popcorntime.android.data.torrent.TorrentEngine 
          return (T) new TorrentEngine(singletonCImpl.provideTorrentCacheDirProvider.get(), singletonCImpl.provideTorrentStreamServerProvider.get());

          case 15: // java.io.File 
          return (T) AppModule_ProvideTorrentCacheDirFactory.provideTorrentCacheDir(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.popcorntime.android.data.torrent.TorrentStreamServer 
          return (T) AppModule_ProvideTorrentStreamServerFactory.provideTorrentStreamServer();

          case 17: // com.popcorntime.android.data.subtitles.SubtitleService 
          return (T) SubtitleModule_ProvideSubtitleServiceFactory.provideSubtitleService(singletonCImpl.provideHttpClientProvider.get(), singletonCImpl.provideOsTokenStoreProvider.get());

          case 18: // com.popcorntime.android.data.subtitles.OsTokenStore 
          return (T) SubtitleModule_ProvideOsTokenStoreFactory.provideOsTokenStore(singletonCImpl.provideOsDataStoreProvider.get());

          case 19: // @javax.inject.Named("osDataStore") androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) SubtitleModule_ProvideOsDataStoreFactory.provideOsDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 20: // com.popcorntime.android.data.cast.CastManager 
          return (T) CastModule_ProvideCastManagerFactory.provideCastManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideKodiCasterProvider.get(), singletonCImpl.provideDlnaCasterProvider.get(), singletonCImpl.provideDlnaDiscoveryProvider.get(), singletonCImpl.provideChromecastCasterProvider.get());

          case 21: // com.popcorntime.android.data.cast.KodiCaster 
          return (T) CastModule_ProvideKodiCasterFactory.provideKodiCaster(singletonCImpl.provideHttpClientProvider.get());

          case 22: // com.popcorntime.android.data.cast.DlnaCaster 
          return (T) CastModule_ProvideDlnaCasterFactory.provideDlnaCaster(singletonCImpl.provideHttpClientProvider.get());

          case 23: // com.popcorntime.android.data.cast.DlnaDiscovery 
          return (T) CastModule_ProvideDlnaDiscoveryFactory.provideDlnaDiscovery(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 24: // com.popcorntime.android.data.cast.ChromecastCaster 
          return (T) CastModule_ProvideChromecastCasterFactory.provideChromecastCaster(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 25: // com.popcorntime.android.data.cast.KodiPrefsStore 
          return (T) CastModule_ProvideKodiPrefsStoreFactory.provideKodiPrefsStore(singletonCImpl.provideCastDataStoreProvider.get());

          case 26: // @javax.inject.Named("castDataStore") androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) CastModule_ProvideCastDataStoreFactory.provideCastDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 27: // com.popcorntime.android.data.remote.PlaybackController 
          return (T) new PlaybackController();

          case 28: // com.popcorntime.android.data.remote.PlaybackQueue 
          return (T) new PlaybackQueue();

          case 29: // com.popcorntime.android.data.remote.RemoteControlServer 
          return (T) new RemoteControlServer(singletonCImpl.playbackControllerProvider.get(), singletonCImpl.playbackQueueProvider.get(), singletonCImpl.torrentEngineProvider.get(), singletonCImpl.provideRemoteControlTokenStoreProvider.get());

          case 30: // com.popcorntime.android.data.remote.RemoteControlTokenStore 
          return (T) RemoteModule_ProvideRemoteControlTokenStoreFactory.provideRemoteControlTokenStore(singletonCImpl.provideRemoteDataStoreProvider.get());

          case 31: // @javax.inject.Named("remoteDataStore") androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) RemoteModule_ProvideRemoteDataStoreFactory.provideRemoteDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 32: // com.popcorntime.android.data.repository.ShowRepositoryImpl 
          return (T) new ShowRepositoryImpl(singletonCImpl.showApiServiceProvider.get(), singletonCImpl.watchedDao(), singletonCImpl.bookmarkedDao(), singletonCImpl.provideTorrentSourcePrefsProvider.get(), singletonCImpl.provideJackettApiServiceProvider.get());

          case 33: // com.popcorntime.android.data.api.ShowApiService 
          return (T) new ShowApiService(singletonCImpl.provideHttpClientProvider.get());

          case 34: // com.popcorntime.android.data.subtitles.OsAuthService 
          return (T) SubtitleModule_ProvideOsAuthServiceFactory.provideOsAuthService(singletonCImpl.provideHttpClientProvider.get());

          case 35: // com.popcorntime.android.data.trakt.TraktAuthService 
          return (T) TraktModule_ProvideTraktAuthServiceFactory.provideTraktAuthService(singletonCImpl.provideTraktHttpClientProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
