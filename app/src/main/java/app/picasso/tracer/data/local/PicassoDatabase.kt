package app.picasso.tracer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class], version = 2, exportSchema = false)
abstract class PicassoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
