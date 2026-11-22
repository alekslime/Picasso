package app.picasso.tracer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class PicassoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
