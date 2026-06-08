package com.popcorntime.android.ui.movies;

import androidx.lifecycle.SavedStateHandle;
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
public final class MovieDetailViewModel_Factory implements Factory<MovieDetailViewModel> {
  private final Provider<MovieRepository> repositoryProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public MovieDetailViewModel_Factory(Provider<MovieRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public MovieDetailViewModel get() {
    return newInstance(repositoryProvider.get(), libraryRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static MovieDetailViewModel_Factory create(Provider<MovieRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new MovieDetailViewModel_Factory(repositoryProvider, libraryRepositoryProvider, savedStateHandleProvider);
  }

  public static MovieDetailViewModel newInstance(MovieRepository repository,
      LibraryRepository libraryRepository, SavedStateHandle savedStateHandle) {
    return new MovieDetailViewModel(repository, libraryRepository, savedStateHandle);
  }
}
