package com.popcorntime.android.ui.movies;

import com.popcorntime.android.domain.repository.LibraryRepository;
import com.popcorntime.android.domain.repository.MovieRepository;
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
public final class MovieBrowserViewModel_Factory implements Factory<MovieBrowserViewModel> {
  private final Provider<MovieRepository> repositoryProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  public MovieBrowserViewModel_Factory(Provider<MovieRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
  }

  @Override
  public MovieBrowserViewModel get() {
    return newInstance(repositoryProvider.get(), libraryRepositoryProvider.get());
  }

  public static MovieBrowserViewModel_Factory create(Provider<MovieRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider) {
    return new MovieBrowserViewModel_Factory(repositoryProvider, libraryRepositoryProvider);
  }

  public static MovieBrowserViewModel newInstance(MovieRepository repository,
      LibraryRepository libraryRepository) {
    return new MovieBrowserViewModel(repository, libraryRepository);
  }
}
