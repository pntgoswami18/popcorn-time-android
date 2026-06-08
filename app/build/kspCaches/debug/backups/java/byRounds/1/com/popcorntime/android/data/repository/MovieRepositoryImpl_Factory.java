package com.popcorntime.android.data.repository;

import com.popcorntime.android.data.api.JackettApiService;
import com.popcorntime.android.data.api.MovieApiService;
import com.popcorntime.android.data.db.dao.BookmarkedDao;
import com.popcorntime.android.data.db.dao.WatchedDao;
import com.popcorntime.android.data.sources.TorrentSourcePrefs;
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
public final class MovieRepositoryImpl_Factory implements Factory<MovieRepositoryImpl> {
  private final Provider<MovieApiService> apiProvider;

  private final Provider<WatchedDao> watchedDaoProvider;

  private final Provider<BookmarkedDao> bookmarkedDaoProvider;

  private final Provider<TorrentSourcePrefs> sourcePrefsProvider;

  private final Provider<JackettApiService> jackettApiProvider;

  public MovieRepositoryImpl_Factory(Provider<MovieApiService> apiProvider,
      Provider<WatchedDao> watchedDaoProvider, Provider<BookmarkedDao> bookmarkedDaoProvider,
      Provider<TorrentSourcePrefs> sourcePrefsProvider,
      Provider<JackettApiService> jackettApiProvider) {
    this.apiProvider = apiProvider;
    this.watchedDaoProvider = watchedDaoProvider;
    this.bookmarkedDaoProvider = bookmarkedDaoProvider;
    this.sourcePrefsProvider = sourcePrefsProvider;
    this.jackettApiProvider = jackettApiProvider;
  }

  @Override
  public MovieRepositoryImpl get() {
    return newInstance(apiProvider.get(), watchedDaoProvider.get(), bookmarkedDaoProvider.get(), sourcePrefsProvider.get(), jackettApiProvider.get());
  }

  public static MovieRepositoryImpl_Factory create(Provider<MovieApiService> apiProvider,
      Provider<WatchedDao> watchedDaoProvider, Provider<BookmarkedDao> bookmarkedDaoProvider,
      Provider<TorrentSourcePrefs> sourcePrefsProvider,
      Provider<JackettApiService> jackettApiProvider) {
    return new MovieRepositoryImpl_Factory(apiProvider, watchedDaoProvider, bookmarkedDaoProvider, sourcePrefsProvider, jackettApiProvider);
  }

  public static MovieRepositoryImpl newInstance(MovieApiService api, WatchedDao watchedDao,
      BookmarkedDao bookmarkedDao, TorrentSourcePrefs sourcePrefs, JackettApiService jackettApi) {
    return new MovieRepositoryImpl(api, watchedDao, bookmarkedDao, sourcePrefs, jackettApi);
  }
}
