package com.meeqat.azan.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_prayer")
data class DailyPrayerEntity(
    @PrimaryKey val date: String,
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val method: String,
    val lat: Double,
    val lng: Double,
)

@Entity(tableName = "manual_offset")
data class ManualOffsetEntity(
    @PrimaryKey val id: Int = 0,
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
)

@Dao
interface DailyPrayerDao {
    @Query("SELECT * FROM daily_prayer WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyPrayerEntity?

    @Query("SELECT * FROM daily_prayer ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyPrayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DailyPrayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DailyPrayerEntity)

    @Query("DELETE FROM daily_prayer WHERE date < :cutoff")
    suspend fun pruneBefore(cutoff: String)
}

@Dao
interface ManualOffsetDao {
    @Query("SELECT * FROM manual_offset WHERE id = 0 LIMIT 1")
    fun observe(): Flow<ManualOffsetEntity?>

    @Query("SELECT * FROM manual_offset WHERE id = 0 LIMIT 1")
    suspend fun get(): ManualOffsetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: ManualOffsetEntity)
}

@Database(
    entities = [DailyPrayerEntity::class, ManualOffsetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun dailyPrayerDao(): DailyPrayerDao
    abstract fun manualOffsetDao(): ManualOffsetDao
}
