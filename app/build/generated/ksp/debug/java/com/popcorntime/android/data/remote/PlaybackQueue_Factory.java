package com.popcorntime.android.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class PlaybackQueue_Factory implements Factory<PlaybackQueue> {
  @Override
  public PlaybackQueue get() {
    return newInstance();
  }

  public static PlaybackQueue_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PlaybackQueue newInstance() {
    return new PlaybackQueue();
  }

  private static final class InstanceHolder {
    private static final PlaybackQueue_Factory INSTANCE = new PlaybackQueue_Factory();
  }
}
