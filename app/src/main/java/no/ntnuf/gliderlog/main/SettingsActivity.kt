package no.ntnuf.gliderlog.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import no.ntnuf.gliderlog.R
import no.ntnuf.gliderlog.common.applyContentInsets
import no.ntnuf.gliderlog.common.applyToolbarInsets
import no.ntnuf.gliderlog.common.enableImmersiveFullscreen

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSettings)
        toolbar.title = getString(R.string.title_activity_settings)
        setSupportActionBar(toolbar)
        applyToolbarInsets(toolbar)
        applyContentInsets(findViewById(R.id.settingsContent))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveFullscreen()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
