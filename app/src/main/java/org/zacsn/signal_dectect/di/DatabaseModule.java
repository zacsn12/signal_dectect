package org.zacsn.signal_dectect.di;

import android.content.Context;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

import org.zacsn.signal_dectect.data.database.AppDatabase;
import org.zacsn.signal_dectect.data.database.BlacklistDao;
import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.WatchlistDao;
import org.zacsn.signal_dectect.data.database.WhitelistDao;

/**
 * Hilt module for providing database-related dependencies.
 * 
 * This module provides:
 * - AppDatabase instance (singleton)
 * - All DAO instances (ScanRecordDao, WatchlistDao, BlacklistDao, WhitelistDao)
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    
    /**
     * Migration from version 1 to 2: Add name column to scan_records table
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add name column to scan_records table
            database.execSQL("ALTER TABLE scan_records ADD COLUMN name TEXT");
        }
    };
    
    /**
     * Provides the singleton AppDatabase instance.
     * 
     * @param context Application context
     * @return AppDatabase instance
     */
    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
            context,
            AppDatabase.class,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2)
        .build();
    }
    
    /**
     * Provides ScanRecordDao for scan record operations.
     * 
     * @param database AppDatabase instance
     * @return ScanRecordDao instance
     */
    @Provides
    @Singleton
    public ScanRecordDao provideScanRecordDao(AppDatabase database) {
        return database.scanRecordDao();
    }
    
    /**
     * Provides WatchlistDao for watchlist operations.
     * 
     * @param database AppDatabase instance
     * @return WatchlistDao instance
     */
    @Provides
    @Singleton
    public WatchlistDao provideWatchlistDao(AppDatabase database) {
        return database.watchlistDao();
    }
    
    /**
     * Provides BlacklistDao for blacklist operations.
     * 
     * @param database AppDatabase instance
     * @return BlacklistDao instance
     */
    @Provides
    @Singleton
    public BlacklistDao provideBlacklistDao(AppDatabase database) {
        return database.blacklistDao();
    }
    
    /**
     * Provides WhitelistDao for whitelist operations.
     * 
     * @param database AppDatabase instance
     * @return WhitelistDao instance
     */
    @Provides
    @Singleton
    public WhitelistDao provideWhitelistDao(AppDatabase database) {
        return database.whitelistDao();
    }
}
