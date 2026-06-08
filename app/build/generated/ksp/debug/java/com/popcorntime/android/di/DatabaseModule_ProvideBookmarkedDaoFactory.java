package com.popcorntime.android.di;

import com.popcorntime.android.data.db.AppDatabase;
import com.popcorntime.android.data.db.dao.BookmarkedDao;
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
public final class DatabaseModule_ProvideBookmarkedDaoFactory implements Factory<BookmarkedDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBookmarkedDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BookmarkedDao get() {
    return provideBookmarkedDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBookmarkedDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBookmarkedDaoFactory(dbProvider);
  }

  public static BookmarkedDao provideBookmarkedDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookmarkedDao(db));
  }
}
