package com.popcorntime.android.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.popcorntime.android.data.db.entity.WatchlistEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class WatchlistDao_Impl implements WatchlistDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WatchlistEntity> __insertionAdapterOfWatchlistEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public WatchlistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWatchlistEntity = new EntityInsertionAdapter<WatchlistEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `watchlist` (`imdbId`,`addedAt`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WatchlistEntity entity) {
        statement.bindString(1, entity.getImdbId());
        statement.bindLong(2, entity.getAddedAt());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM watchlist WHERE imdbId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final WatchlistEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWatchlistEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String imdbId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, imdbId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WatchlistEntity>> observeAll() {
    final String _sql = "SELECT * FROM watchlist ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"watchlist"}, new Callable<List<WatchlistEntity>>() {
      @Override
      @NonNull
      public List<WatchlistEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfImdbId = CursorUtil.getColumnIndexOrThrow(_cursor, "imdbId");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<WatchlistEntity> _result = new ArrayList<WatchlistEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WatchlistEntity _item;
            final String _tmpImdbId;
            _tmpImdbId = _cursor.getString(_cursorIndexOfImdbId);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new WatchlistEntity(_tmpImdbId,_tmpAddedAt);
            _result.add(_item);
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
  public Object isInWatchlist(final String imdbId,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT COUNT(*) > 0 FROM watchlist WHERE imdbId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, imdbId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
