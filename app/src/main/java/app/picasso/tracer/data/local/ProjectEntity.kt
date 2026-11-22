package app.picasso.tracer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val opacity: Float = 0.5f,
    val filter: String = "ORIGINAL",
    val gridVisible: Boolean = false,
    val gridRows: Int = 3,
    val gridCols: Int = 3,
    val favorite: Boolean = false,
)
