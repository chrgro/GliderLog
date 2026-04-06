package no.ntnuf.gliderlog.common

import android.content.res.ColorStateList
import android.view.View

object ColoringUtil {
    fun colorMe(view: View, color: Int) {
        // Min SDK is 24, so we can use background tinting directly.
        view.backgroundTintList = ColorStateList.valueOf(color)
    }
}

