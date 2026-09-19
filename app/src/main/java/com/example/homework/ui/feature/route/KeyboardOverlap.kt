package com.example.homework.ui.feature.route

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun rememberKeyboardOverlapPx(): Int {
    val view = LocalView.current
    var overlap by remember { mutableIntStateOf(0) }
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val visible = Rect()
            view.getWindowVisibleDisplayFrame(visible)
            val insets = ViewCompat.getRootWindowInsets(view)
            val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val nav = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            val frameOverlap = (view.rootView.height - visible.bottom - nav).coerceAtLeast(0)
            overlap = maxOf(ime, frameOverlap)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        listener.onGlobalLayout()
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return overlap
}
