package app.picasso.tracer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class, LayerEntity::class], version = 7, exportSchema = false)
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE layers ADD COLUMN opacity REAL NOT NULL DEFAULT 1.0")
            }
        }

        /**
         * Layers get their own placement and filter (they used to share the project's) plus a
         * visibility flag. Existing layers start with a copy of their project's placement and
         * filter, so they look exactly as before, and hidden, so no project suddenly shows a
         * stack of overlays; layers added from now on are visible.
         */
        val MIGRATION_5_6_SQL = listOf(
            "ALTER TABLE layers ADD COLUMN offsetX REAL NOT NULL DEFAULT 0.0",
            "ALTER TABLE layers ADD COLUMN offsetY REAL NOT NULL DEFAULT 0.0",
            "ALTER TABLE layers ADD COLUMN scale REAL NOT NULL DEFAULT 1.0",
            "ALTER TABLE layers ADD COLUMN rotationDeg REAL NOT NULL DEFAULT 0.0",
            "ALTER TABLE layers ADD COLUMN flipH INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE layers ADD COLUMN flipV INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE layers ADD COLUMN filter TEXT NOT NULL DEFAULT 'ORIGINAL'",
            "ALTER TABLE layers ADD COLUMN visible INTEGER NOT NULL DEFAULT 0",
            "UPDATE layers SET " +
                "offsetX = (SELECT offsetX FROM projects WHERE projects.id = layers.projectId), " +
                "offsetY = (SELECT offsetY FROM projects WHERE projects.id = layers.projectId), " +
                "scale = (SELECT scale FROM projects WHERE projects.id = layers.projectId), " +
                "rotationDeg = (SELECT rotationDeg FROM projects WHERE projects.id = layers.projectId), " +
                "flipH = (SELECT flipH FROM projects WHERE projects.id = layers.projectId), " +
                "flipV = (SELECT flipV FROM projects WHERE projects.id = layers.projectId), " +
                "filter = (SELECT filter FROM projects WHERE projects.id = layers.projectId)",
        )

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_5_6_SQL.forEach { db.execSQL(it) }
            }
        }

        /** The base image gets an eye toggle like the layers; every existing project keeps it on. */
        val MIGRATION_6_7_SQL = listOf(
            "ALTER TABLE projects ADD COLUMN baseVisible INTEGER NOT NULL DEFAULT 1",
        )

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_6_7_SQL.forEach { db.execSQL(it) }
            }
        }

        /** Every migration, in order; both database builders use this so none is forgotten. */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )
    }
}
