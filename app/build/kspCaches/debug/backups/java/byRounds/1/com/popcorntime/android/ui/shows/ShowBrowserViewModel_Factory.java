package com.popcorntime.android.ui.shows;

import com.popcorntime.android.domain.repository.LibraryRepository;
import com.popcorntime.android.domain.repository.ShowRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ShowBrowserViewModel_Factory implements Factory<ShowBrowserViewModel> {
  private final Provider<ShowRepository> repositoryProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  public ShowBrowserViewModel_Factory(Provider<ShowRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
  }

  @Override
  public ShowBrowserViewModel get() {
    return newInstance(repositoryProvider.get(), libraryRepositoryProvider.get());
  }

  public static ShowBrowserViewModel_Factory create(Provider<ShowRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    return new ShowBrowserViewModel_Factory(repositoryProvider, libraryRepositoryProvider);
  }

  public static ShowBrowserViewModel newInstance(ShowRepository repository,
      LibraryRepository libraryRepository) {
    return new ShowBrowserViewModel(repository, libraryRepository);
  }
}
