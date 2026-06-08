package com.popcorntime.android.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.popcorntime.android.data.db.entity.LibraryItemEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LibraryItemDao_Impl implements LibraryItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LibraryItemEntity> __insertionAdapterOfLibraryItemEntity;

  public LibraryItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLibraryItemEntity = new EntityInsertionAdapter<LibraryItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `library_items` (`imdbId`,`title`,`posterUrl`,`year`,`contentType`,`addedAt`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LibraryItemEntity entity) {
        statement.bindString(1, entity.getImdbId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getPosterUrl());
        statement.bindString(4, entity.getYear());
        statement.bindString(5, entity.getContentType());
        statement.bindLong(6, entity.getAddedAt());
      }
    };
  }

  @Override
  public Object upsert(final LibraryItemEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLibraryItemEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LibraryItemEntity>> getByIds(final List<String> ids) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM library_items WHERE imdbId IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : ids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"library_items"}, new Callable<List<LibraryItemEntity>>() {
      @Override
      @NonNull
      public List<LibraryItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<LibraryItemEntity> _result = new ArrayList<LibraryItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LibraryItemEntity _item_1;
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPosterUrl;
            _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            final String _tmpYear;
            _tmpYear = _cursor.getString(_cursorIndexOfYear);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item_1 = new LibraryItemEntity(_tmpImdbId,_tmpTitle,_tmpPosterUrl,_tmpYear,_tmpContentType,_tmpAddedAt);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final String imdbId,
      final Continuation<? super LibraryItemEntity> $completion) {
    final String _sql = "SELECT * FROM library_items WHERE imdbId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LibraryItemEntity>() {
      @Override
      @Nullable
      public LibraryItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfPosterUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "posterUrl");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final LibraryItemEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpPosterUrl;
            _tmpPosterUrl = _cursor.getString(_cursorIndexOfPosterUrl);
            final String _tmpYear;
            _tmpYear = _cursor.getString(_cursorIndexOfYear);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _result = new LibraryItemEntity(_tmpImdbId,_tmpTitle,_tmpPosterUrl,_tmpYear,_tmpContentType,_tmpAddedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
