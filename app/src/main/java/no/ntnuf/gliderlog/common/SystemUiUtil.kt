package no.ntnuf.gliderlog.common

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun AppCompatActivity.enableImmersiveFullscreen() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}

fun applyToolbarInsets(toolbar: View) {
    val initialPaddingLeft = toolbar.paddingLeft
    val initialPaddingTop = toolbar.paddingTop
    val initialPaddingRight = toolbar.paddingRight
    val initialPaddingBottom = toolbar.paddingBottom
    val initialHeight = toolbar.layoutParams.height

    ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
        val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(
            initialPaddingLeft,
            initialPaddingTop + statusBars.top,
            initialPaddingRight,
            initialPaddingBottom,
        )
        if (initialHeight > 0) {
            view.layoutParams = view.layoutParams.apply {
                height = initialHeight + statusBars.top
            }
        }
        insets
    }
    ViewCompat.requestApplyInsets(toolbar)
}

fun applyContentInsets(content: View) {
    val initialPaddingLeft = content.paddingLeft
    val initialPaddingTop = content.paddingTop
    val initialPaddingRight = content.paddingRight
    val initialPaddingBottom = content.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            initialPaddingLeft + systemBars.left,
            initialPaddingTop,
            initialPaddingRight + systemBars.right,
            initialPaddingBottom + systemBars.bottom,
        )
        insets
    }
    ViewCompat.requestApplyInsets(content)
}

fun applyFabInsets(fab: View) {
    val layoutParams = fab.layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val initialLeftMargin = layoutParams.leftMargin
    val initialTopMargin = layoutParams.topMargin
    val initialRightMargin = layoutParams.rightMargin
    val initialBottomMargin = layoutParams.bottomMargin

    ViewCompat.setOnApplyWindowInsetsListener(fab) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val margins = view.layoutParams as? ViewGroup.MarginLayoutParams
        margins?.setMargins(
            initialLeftMargin + systemBars.left,
            initialTopMargin,
            initialRightMargin + systemBars.right,
            initialBottomMargin + systemBars.bottom,
        )
        if (margins != null) {
            view.layoutParams = margins
        }
        insets
    }
    ViewCompat.requestApplyInsets(fab)
}

