package com.popcorntime.android.di;

import com.popcorntime.android.data.db.AppDatabase;
import com.popcorntime.android.data.db.dao.LibraryItemDao;
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
public final class DatabaseModule_ProvideLibraryItemDaoFactory implements Factory<LibraryItemDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideLibraryItemDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public LibraryItemDao get() {
    return provideLibraryItemDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideLibraryItemDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideLibraryItemDaoFactory(dbProvider);
  }

  public static LibraryItemDao provideLibraryItemDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLibraryItemDao(db));
  }
}
