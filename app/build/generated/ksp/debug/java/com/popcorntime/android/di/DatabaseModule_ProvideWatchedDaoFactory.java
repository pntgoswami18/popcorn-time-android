package com.popcorntime.android.di;

import com.popcorntime.android.data.db.AppDatabase;
import com.popcorntime.android.data.db.dao.WatchedDao;
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
public final class DatabaseModule_ProvideWatchedDaoFactory implements Factory<WatchedDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideWatchedDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public WatchedDao get() {
    return provideWatchedDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideWatchedDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideWatchedDaoFactory(dbProvider);
  }

  public static WatchedDao provideWatchedDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWatchedDao(db));
  }
}
