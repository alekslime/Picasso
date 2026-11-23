package app.picasso.tracer.data

/** Removes stored reference images; an interface so repositories stay JVM-testable. */
interface ImageFiles {
    suspend fun delete(path: String)
}
