package app.picasso.tracer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Extra reference images stacked on one project (sketch, value study, color
 * pass…). The project's own image stays the base; layers are selected, never
 * blended by default. Each layer has its own opacity, placement and filter.
 */
@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class LayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val imagePath: String,
    val name: String,
    val createdAt: Long,
    /** Fraction of the layer drawn; the base image keeps the project's opacity. */
    val opacity: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val filter: String = "ORIGINAL",
    val visible: Boolean = true,
    val sortOrder: Int = 0,
)
