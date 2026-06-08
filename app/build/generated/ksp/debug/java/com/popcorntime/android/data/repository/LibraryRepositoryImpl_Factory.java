package com.popcorntime.android.data.repository;

import com.popcorntime.android.data.db.dao.BookmarkedDao;
import com.popcorntime.android.data.db.dao.LibraryItemDao;
import com.popcorntime.android.data.db.dao.WatchedDao;
import com.popcorntime.android.data.db.dao.WatchlistDao;
import com.popcorntime.android.data.trakt.TraktSyncService;
import com.popcorntime.android.data.trakt.TraktTokenStore;
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
public final class LibraryRepositoryImpl_Factory implements Factory<LibraryRepositoryImpl> {
  private final Provider<BookmarkedDao> bookmarkedDaoProvider;

  private final Provider<WatchedDao> watchedDaoProvider;

  private final Provider<WatchlistDao> watchlistDaoProvider;

  private final Provider<LibraryItemDao> libraryItemDaoProvider;

  private final Provider<TraktSyncService> traktSyncServiceProvider;

  private final Provider<TraktTokenStore> traktTokenStoreProvider;

  public LibraryRepositoryImpl_Factory(Provider<BookmarkedDao> bookmarkedDaoProvider,
      Provider<WatchedDao> watchedDaoProvider, Provider<WatchlistDao> watchlistDaoProvider,
      Provider<LibraryItemDao> libraryItemDaoProvider,
      Provider<TraktSyncService> traktSyncServiceProvider,
      Provider<TraktTokenStore> traktTokenStoreProvider) {
    this.bookmarkedDaoProvider = bookmarkedDaoProvider;
    this.watchedDaoProvider = watchedDaoProvider;
    this.watchlistDaoProvider = watchlistDaoProvider;
    this.libraryItemDaoProvider = libraryItemDaoProvider;
    this.traktSyncServiceProvider = traktSyncServiceProvider;
    this.traktTokenStoreProvider = traktTokenStoreProvider;
  }

  @Override
  public LibraryRepositoryImpl get() {
    return newInstance(bookmarkedDaoProvider.get(), watchedDaoProvider.get(), watchlistDaoProvider.get(), libraryItemDaoProvider.get(), traktSyncServiceProvider.get(), traktTokenStoreProvider.get());
  }

  public static LibraryRepositoryImpl_Factory create(Provider<BookmarkedDao> bookmarkedDaoProvider,
      Provider<WatchedDao> watchedDaoProvider, Provider<WatchlistDao> watchlistDaoProvider,
      Provider<LibraryItemDao> libraryItemDaoProvider,
      Provider<TraktSyncService> traktSyncServiceProvider,
      Provider<TraktTokenStore> traktTokenStoreProvider) {
    return new LibraryRepositoryImpl_Factory(bookmarkedDaoProvider, watchedDaoProvider, watchlistDaoProvider, libraryItemDaoProvider, traktSyncServiceProvider, traktTokenStoreProvider);
  }

  public static LibraryRepositoryImpl newInstance(BookmarkedDao bookmarkedDao,
      WatchedDao watchedDao, WatchlistDao watchlistDao, LibraryItemDao libraryItemDao,
      TraktSyncService traktSyncService, TraktTokenStore traktTokenStore) {
    return new LibraryRepositoryImpl(bookmarkedDao, watchedDao, watchlistDao, libraryItemDao, traktSyncService, traktTokenStore);
  }
}
