package app.picasso.tracer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Upsert
    suspend fun upsert(entity: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM layers WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeLayers(projectId: Long): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE projectId = :projectId")
    suspend fun getLayers(projectId: Long): List<LayerEntity>

    @Upsert
    suspend fun upsertLayer(entity: LayerEntity): Long

    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getLayerById(id: Long): LayerEntity?

    @Query("DELETE FROM layers WHERE id = :id")
    suspend fun deleteLayerById(id: Long)
}
