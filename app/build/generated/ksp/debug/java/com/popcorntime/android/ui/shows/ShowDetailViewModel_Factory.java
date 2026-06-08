package com.popcorntime.android.ui.shows;

import androidx.lifecycle.SavedStateHandle;
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
public final class ShowDetailViewModel_Factory implements Factory<ShowDetailViewModel> {
  private final Provider<ShowRepository> repositoryProvider;

  private final Provider<LibraryRepository> libraryRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ShowDetailViewModel_Factory(Provider<ShowRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.libraryRepositoryProvider = libraryRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ShowDetailViewModel get() {
    return newInstance(repositoryProvider.get(), libraryRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ShowDetailViewModel_Factory create(Provider<ShowRepository> repositoryProvider,
      Provider<LibraryRepository> libraryRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ShowDetailViewModel_Factory(repositoryProvider, libraryRepositoryProvider, savedStateHandleProvider);
  }

  public static ShowDetailViewModel newInstance(ShowRepository repository,
      LibraryRepository libraryRepository, SavedStateHandle savedStateHandle) {
    return new ShowDetailViewModel(repository, libraryRepository, savedStateHandle);
  }
}
