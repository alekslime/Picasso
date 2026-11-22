package app.picasso.tracer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import app.picasso.tracer.MainActivity
import app.picasso.tracer.R
import app.picasso.tracer.data.local.PicassoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Continue last tracing" home-screen widget. Minimal RemoteViews on
 * purpose (no Glance): it reads the newest project and deep-links to it.
 */
class PicassoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(context, PicassoDatabase::class.java, "picasso.db")
                    .addMigrations(PicassoDatabase.MIGRATION_1_2, PicassoDatabase.MIGRATION_2_3)
                    .build()
                val latest = runCatching { db.projectDao().observeAll().first().firstOrNull() }.getOrNull()
                db.close()

                val views = RemoteViews(context.packageName, R.layout.widget_picasso)
                views.setTextViewText(
                    R.id.widget_project,
                    latest?.name ?: context.getString(R.string.widget_no_project),
                )
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (latest != null) putExtra(MainActivity.EXTRA_OPEN_PROJECT_ID, latest.id)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                manager.updateAppWidget(widgetId, views)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Asks the system to refresh every placed widget (e.g. after a save). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PicassoWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, PicassoWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
