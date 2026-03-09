package com.github.kr328.clash.design.util

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View

/**
 * Apply a frosted-glass blur effect to the view's background on API 31+.
 * On older devices this is a no-op (the translucent drawables already simulate glass).
 *
 * @param radiusX horizontal blur radius in pixels (default 25)
 * @param radiusY vertical blur radius in pixels (default 25)
 */
fun View.applyGlassBlur(radiusX: Float = 25f, radiusY: Float = 25f) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(
            RenderEffect.createBlurEffect(radiusX, radiusY, Shader.TileMode.CLAMP)
        )
    }
}

/**
 * Remove any render effect previously applied.
 */
fun View.clearGlassBlur() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setRenderEffect(null)
    }
}
