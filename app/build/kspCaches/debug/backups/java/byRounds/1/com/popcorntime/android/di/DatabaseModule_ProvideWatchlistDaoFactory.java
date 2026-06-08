package com.popcorntime.android.di;

import com.popcorntime.android.data.db.AppDatabase;
import com.popcorntime.android.data.db.dao.WatchlistDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideWatchlistDaoFactory implements Factory<WatchlistDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideWatchlistDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public WatchlistDao get() {
    return provideWatchlistDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideWatchlistDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideWatchlistDaoFactory(dbProvider);
  }

  public static WatchlistDao provideWatchlistDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWatchlistDao(db));
  }
}
