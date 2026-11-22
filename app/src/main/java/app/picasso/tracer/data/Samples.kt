package app.picasso.tracer.data

import app.picasso.tracer.R

/** Bundled starter image shipped under assets/. */
data class Sample(
    val assetPath: String,
    val nameRes: Int,
)

object Samples {
    val all = listOf(
        Sample("samples/rose.png", R.string.sample_rose),
        Sample("samples/eye.png", R.string.sample_eye),
        Sample("samples/mountains.png", R.string.sample_mountains),
    )
}
