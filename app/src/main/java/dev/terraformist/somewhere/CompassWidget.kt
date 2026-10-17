package dev.terraformist.somewhere

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The home-screen widget, in two sizes ([Small] 2×2 and [Wide] 4×2) that share everything.
 * Both work at any size: wide shapes get the side-by-side layout, square-ish ones the stacked one.
 *
 * It updates every half hour, whenever the app is closed, and (with "Allow all the time"
 * location) whenever Android notices the phone has moved.
 */
open class CompassWidget : AppWidgetProvider() {

    class Small : CompassWidget()
    class Wide : CompassWidget()

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshAll(context)
        Where.followInBackground(context)
        val pending = goAsync()
        Where.refresh(context) {
            refreshAll(context)
            pending.finish()
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        draw(context, manager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Where.ACTION_MOVED) {
            Where.fromMovedIntent(intent)?.let { Where.save(context, it) }
            refreshAll(context)
            return
        }
        super.onReceive(context, intent)
    }

    companion object {

        /** Redraw every copy of both widgets. */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            for (kind in listOf(Small::class.java, Wide::class.java)) {
                for (id in manager.getAppWidgetIds(ComponentName(context, kind))) draw(context, manager, id)
            }
        }

        private fun draw(context: Context, manager: AppWidgetManager, id: Int) {
            val (widthDp, heightDp) = sizeDp(manager.getAppWidgetOptions(id))
            val density = context.resources.displayMetrics.density
            // Keep the picture a sensible size; it's scaled to fit anyway.
            val scale = minOf(density, MAX_SIDE_PX / max(widthDp, heightDp))
            val width = (widthDp * scale).roundToInt()
            val height = (heightDp * scale).roundToInt()

            val allowed = Where.hasLocation(context)
            val reading = if (allowed) Where.reading(context) else null
            val mode = when {
                !allowed -> Mode.NO_PERMISSION
                reading == null -> Mode.LOOKING
                else -> Mode.READY
            }
            val open = reading?.store?.isOpen() == true
            val picture = Art.widget(context, width, height, reading, mode, open)

            val views = RemoteViews(context.packageName, R.layout.widget_compass)
            views.setImageViewBitmap(R.id.art, picture)
            views.setContentDescription(R.id.art, Art.describe(reading, mode))
            views.setOnClickPendingIntent(R.id.art, openApp(context))
            manager.updateAppWidget(id, views)
        }

        /** The widget's size on screen, in dp, as the home screen reports it (portrait). */
        private fun sizeDp(options: Bundle): Pair<Float, Float> {
            val sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            val first = sizes?.firstOrNull()
            if (first != null && first.width > 0 && first.height > 0) return first.width to first.height
            val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 170)
            val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 170)
            return w.coerceAtLeast(40).toFloat() to h.coerceAtLeast(40).toFloat()
        }

        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, CompassActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private const val MAX_SIDE_PX = 900f
    }
}
