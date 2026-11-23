package app.picasso.tracer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Extra reference images stacked on one project (sketch, value study, color
 * pass…). The project's own image stays the base; layers are selected, never
 * blended, and share the project's transform, opacity and filter.
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
)
