package com.popcorntime.android.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.popcorntime.android.data.db.dao.BookmarkedDao;
import com.popcorntime.android.data.db.dao.BookmarkedDao_Impl;
import com.popcorntime.android.data.db.dao.LibraryItemDao;
import com.popcorntime.android.data.db.dao.LibraryItemDao_Impl;
import com.popcorntime.android.data.db.dao.WatchedDao;
import com.popcorntime.android.data.db.dao.WatchedDao_Impl;
import com.popcorntime.android.data.db.dao.WatchlistDao;
import com.popcorntime.android.data.db.dao.WatchlistDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile WatchedDao _watchedDao;

  private volatile BookmarkedDao _bookmarkedDao;

  private volatile WatchlistDao _watchlistDao;

  private volatile LibraryItemDao _libraryItemDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `watched` (`imdbId` TEXT NOT NULL, `watchedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarked` (`imdbId` TEXT NOT NULL, `bookmarkedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`imdbId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `library_items` (`imdbId` TEXT NOT NULL, `title` TEXT NOT NULL, `posterUrl` TEXT NOT NULL, `year` TEXT NOT NULL, `contentType` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`imdbId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'bbccd474d2031b8868cf1cd65259027c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `watched`");
        db.execSQL("DROP TABLE IF EXISTS `bookmarked`");
        db.execSQL("DROP TABLE IF EXISTS `watchlist`");
        db.execSQL("DROP TABLE IF EXISTS `library_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsWatched = new HashMap<String, TableInfo.Column>(2);
        _columnsWatched.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatched.put("watchedAt", new TableInfo.Column("watchedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatched = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatched = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWatched = new TableInfo("watched", _columnsWatched, _foreignKeysWatched, _indicesWatched);
        final TableInfo _existingWatched = TableInfo.read(db, "watched");
        if (!_infoWatched.equals(_existingWatched)) {
          return new RoomOpenHelper.ValidationResult(false, "watched(com.popcorntime.android.data.db.entity.WatchedEntity).\n"
                  + " Expected:\n" + _infoWatched + "\n"
                  + " Found:\n" + _existingWatched);
        }
        final HashMap<String, TableInfo.Column> _columnsBookmarked = new HashMap<String, TableInfo.Column>(2);
        _columnsBookmarked.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarked.put("bookmarkedAt", new TableInfo.Column("bookmarkedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBookmarked = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBookmarked = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBookmarked = new TableInfo("bookmarked", _columnsBookmarked, _foreignKeysBookmarked, _indicesBookmarked);
        final TableInfo _existingBookmarked = TableInfo.read(db, "bookmarked");
        if (!_infoBookmarked.equals(_existingBookmarked)) {
          return new RoomOpenHelper.ValidationResult(false, "bookmarked(com.popcorntime.android.data.db.entity.BookmarkedEntity).\n"
                  + " Expected:\n" + _infoBookmarked + "\n"
                  + " Found:\n" + _existingBookmarked);
        }
        final HashMap<String, TableInfo.Column> _columnsWatchlist = new HashMap<String, TableInfo.Column>(2);
        _columnsWatchlist.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchlist.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatchlist = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatchlist = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWatchlist = new TableInfo("watchlist", _columnsWatchlist, _foreignKeysWatchlist, _indicesWatchlist);
        final TableInfo _existingWatchlist = TableInfo.read(db, "watchlist");
        if (!_infoWatchlist.equals(_existingWatchlist)) {
          return new RoomOpenHelper.ValidationResult(false, "watchlist(com.popcorntime.android.data.db.entity.WatchlistEntity).\n"
                  + " Expected:\n" + _infoWatchlist + "\n"
                  + " Found:\n" + _existingWatchlist);
        }
        final HashMap<String, TableInfo.Column> _columnsLibraryItems = new HashMap<String, TableInfo.Column>(6);
        _columnsLibraryItems.put("imdbId", new TableInfo.Column("imdbId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItems.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItems.put("posterUrl", new TableInfo.Column("posterUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItems.put("year", new TableInfo.Column("year", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItems.put("contentType", new TableInfo.Column("contentType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLibraryItems.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLibraryItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLibraryItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLibraryItems = new TableInfo("library_items", _columnsLibraryItems, _foreignKeysLibraryItems, _indicesLibraryItems);
        final TableInfo _existingLibraryItems = TableInfo.read(db, "library_items");
        if (!_infoLibraryItems.equals(_existingLibraryItems)) {
          return new RoomOpenHelper.ValidationResult(false, "library_items(com.popcorntime.android.data.db.entity.LibraryItemEntity).\n"
                  + " Expected:\n" + _infoLibraryItems + "\n"
                  + " Found:\n" + _existingLibraryItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "bbccd474d2031b8868cf1cd65259027c", "124d286eb16fdb10ed8c863557bd1eb4");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "watched","bookmarked","watchlist","library_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `watched`");
      _db.execSQL("DELETE FROM `bookmarked`");
      _db.execSQL("DELETE FROM `watchlist`");
      _db.execSQL("DELETE FROM `library_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WatchedDao.class, WatchedDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BookmarkedDao.class, BookmarkedDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WatchlistDao.class, WatchlistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LibraryItemDao.class, LibraryItemDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WatchedDao watchedDao() {
    if (_watchedDao != null) {
      return _watchedDao;
    } else {
      synchronized(this) {
        if(_watchedDao == null) {
          _watchedDao = new WatchedDao_Impl(this);
        }
        return _watchedDao;
      }
    }
  }

  @Override
  public BookmarkedDao bookmarkedDao() {
    if (_bookmarkedDao != null) {
      return _bookmarkedDao;
    } else {
      synchronized(this) {
        if(_bookmarkedDao == null) {
          _bookmarkedDao = new BookmarkedDao_Impl(this);
        }
        return _bookmarkedDao;
      }
    }
  }

  @Override
  public WatchlistDao watchlistDao() {
    if (_watchlistDao != null) {
      return _watchlistDao;
    } else {
      synchronized(this) {
        if(_watchlistDao == null) {
          _watchlistDao = new WatchlistDao_Impl(this);
        }
        return _watchlistDao;
      }
    }
  }

  @Override
  public LibraryItemDao libraryItemDao() {
    if (_libraryItemDao != null) {
      return _libraryItemDao;
    } else {
      synchronized(this) {
        if(_libraryItemDao == null) {
          _libraryItemDao = new LibraryItemDao_Impl(this);
        }
        return _libraryItemDao;
      }
    }
  }
}
