package dev.terraformist.somewhere

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import java.util.Locale

/** The app itself: the full-screen compass, with a button for directions. */
class CompassActivity : Activity() {

    private lateinit var compass: CompassView
    private lateinit var directions: TextView
    private lateinit var widgetHelp: TextView

    private lateinit var heading: Heading
    private var watcher: LocationListener? = null
    private var askedThisTime = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)
        compass = findViewById(R.id.compass)
        directions = findViewById(R.id.directions)
        widgetHelp = findViewById(R.id.widget_help)

        directions.typeface = Art.typeface(this, isBold = true)
        widgetHelp.typeface = Art.typeface(this, isBold = false)

        heading = Heading(this, Facing.SCREEN) {
            compass.heading = it
            compass.invalidate()
        }

        setUpAbout()
        directions.setOnClickListener { onDirections() }
        widgetHelp.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), ASK_BACKGROUND)
        }
    }

    // ---------- About card (tap the title) ----------

    private lateinit var aboutScrim: View
    private val closeAboutOnBack = OnBackInvokedCallback { hideAbout() }

    private fun setUpAbout() {
        aboutScrim = findViewById(R.id.about_scrim)
        val titleTap = findViewById<View>(R.id.title_tap)

        val regular = Art.typeface(this, isBold = false)
        for (id in intArrayOf(R.id.about_line1, R.id.about_line2, R.id.about_tips, R.id.about_signoff)) {
            findViewById<TextView>(id).typeface = regular
        }
        findViewById<TextView>(R.id.about_ok).typeface = Art.typeface(this, isBold = true)
        findViewById<DotTextView>(R.id.about_headline).text = getString(R.string.about_headline)

        // Keep the invisible tap area sitting exactly on the drawn title, whatever the screen size.
        compass.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val minHeight = 48f * resources.displayMetrics.density
            val width = (compass.titleWidth() + 32f * resources.displayMetrics.density).toInt()
            val params = titleTap.layoutParams as FrameLayout.LayoutParams
            val top = (compass.titleCenterY() - minHeight / 2f).toInt().coerceAtLeast(0)
            if (params.width != width || params.topMargin != top) {
                params.width = width
                params.height = minHeight.toInt()
                params.topMargin = top
                // Not during this layout pass: on the next frame.
                titleTap.post { titleTap.layoutParams = params }
            }
        }

        titleTap.setOnClickListener { showAbout() }
        aboutScrim.setOnClickListener { hideAbout() }
        findViewById<View>(R.id.about_ok).setOnClickListener { hideAbout() }
    }

    private fun showAbout() {
        if (aboutScrim.visibility == View.VISIBLE) return
        aboutScrim.alpha = 0f
        aboutScrim.visibility = View.VISIBLE
        aboutScrim.animate().alpha(1f).setDuration(160L).start()
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            closeAboutOnBack,
        )
        findViewById<View>(R.id.about_headline).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    private fun hideAbout() {
        if (aboutScrim.visibility != View.VISIBLE) return
        onBackInvokedDispatcher.unregisterOnBackInvokedCallback(closeAboutOnBack)
        aboutScrim.animate().alpha(0f).setDuration(120L).withEndAction {
            aboutScrim.visibility = View.GONE
        }.start()
    }

    override fun onResume() {
        super.onResume()
        if (!Where.hasLocation(this) && !askedThisTime) {
            askedThisTime = true
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                ASK_LOCATION,
            )
        }
        start()
    }

    override fun onPause() {
        super.onPause()
        Where.stop(this, watcher)
        watcher = null
        heading.stop()
        // Leave the widget showing the latest position.
        CompassWidget.refreshAll(this)
    }

    private fun start() {
        if (Where.hasBackground(this)) Where.followInBackground(this)
        show(Where.last(this))
        if (!Where.hasLocation(this)) return
        if (watcher == null) {
            watcher = Where.watch(this, everyMs = 2_000L) { show(it) }
        }
        Where.refresh(this) { fresh -> if (fresh != null) show(fresh) }
        heading.start()
    }

    private fun show(location: Location?) {
        val allowed = Where.hasLocation(this)
        val reading = if (allowed) location?.let { Geo.nearest(it.latitude, it.longitude) } else null
        location?.let { heading.setLocation(it) }

        compass.reading = reading
        compass.mode = when {
            !allowed -> Mode.NO_PERMISSION
            reading == null -> Mode.LOOKING
            else -> Mode.READY
        }
        compass.open = reading?.store?.isOpen() == true
        compass.update()

        directions.text = when {
            !allowed -> "ALLOW LOCATION"
            reading != null && reading.meters < WALKING_METERS -> "WALKING DIRECTIONS"
            else -> "DIRECTIONS"
        }
        directions.isEnabled = !allowed || reading != null
        directions.alpha = if (directions.isEnabled) 1f else 0.4f

        // Only offered when it would help: location is on, but only while the app is open.
        widgetHelp.visibility = if (allowed && !Where.hasBackground(this)) View.VISIBLE else View.GONE
    }

    private fun onDirections() {
        if (!Where.hasLocation(this)) {
            // They said no before, so Android won't ask again: open this app's settings instead.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
            )
            return
        }
        val reading = compass.reading ?: return
        val store = reading.store
        var url = String.format(
            Locale.US,
            "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f",
            store.latitude,
            store.longitude,
        )
        if (reading.meters < WALKING_METERS) url += "&travelmode=walking"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: android.content.ActivityNotFoundException) {
            // No maps app or browser: nothing sensible to do.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            start()
            CompassWidget.refreshAll(this)
        } else {
            show(Where.last(this))
        }
    }

    private companion object {
        const val ASK_LOCATION = 1
        const val ASK_BACKGROUND = 2

        /** Closer than this, directions are for walking. */
        const val WALKING_METERS = 5_000.0
    }
}
