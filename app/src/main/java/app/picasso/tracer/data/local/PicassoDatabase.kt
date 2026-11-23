package app.picasso.tracer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class, LayerEntity::class], version = 4, exportSchema = false)
abstract class PicassoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN guideType TEXT NOT NULL DEFAULT 'GRID'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS layers (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "projectId INTEGER NOT NULL, " +
                        "imagePath TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_layers_projectId ON layers(projectId)")
            }
        }
    }
}
