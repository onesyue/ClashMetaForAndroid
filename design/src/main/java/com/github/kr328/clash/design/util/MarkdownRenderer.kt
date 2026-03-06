package com.github.kr328.clash.design.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

/**
 * Minimal Markdown → Spanned renderer.
 * Supports: # h1, ## h2, **bold**, *italic*, - bullet, blank line separation.
 */
object MarkdownRenderer {

    fun render(markdown: String): CharSequence {
        if (markdown.isBlank()) return ""

        val ssb = SpannableStringBuilder()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trimEnd()
            i++

            when {
                // H1
                trimmed.startsWith("# ") -> {
                    val text = trimmed.removePrefix("# ").trim()
                    val start = ssb.length
                    ssb.append(text)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(1.3f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.append("\n")
                }
                // H2
                trimmed.startsWith("## ") -> {
                    val text = trimmed.removePrefix("## ").trim()
                    val start = ssb.length
                    ssb.append(text)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(1.15f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.append("\n")
                }
                // H3
                trimmed.startsWith("### ") -> {
                    val text = trimmed.removePrefix("### ").trim()
                    val start = ssb.length
                    ssb.append(text)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.append("\n")
                }
                // Bullet - or *
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val text = trimmed.drop(2).trim()
                    val start = ssb.length
                    appendInline(ssb, text)
                    ssb.append("\n")
                    ssb.setSpan(BulletSpan(12), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Blank line → paragraph break
                trimmed.isEmpty() -> {
                    if (ssb.isNotEmpty() && ssb.last() != '\n') ssb.append("\n")
                    ssb.append("\n")
                }
                // Normal paragraph line
                else -> {
                    appendInline(ssb, trimmed)
                    ssb.append("\n")
                }
            }
        }

        // Trim trailing newlines
        while (ssb.isNotEmpty() && ssb.last() == '\n') {
            ssb.delete(ssb.length - 1, ssb.length)
        }

        return ssb
    }

    /** Parse inline **bold** and *italic* within a line segment */
    private fun appendInline(ssb: SpannableStringBuilder, text: String) {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldIdx   = remaining.indexOf("**")
            val italicIdx = remaining.indexOf("*").let { if (it >= 0 && remaining.getOrNull(it + 1) == '*') -1 else it }

            val nextBold   = if (boldIdx >= 0) boldIdx else Int.MAX_VALUE
            val nextItalic = if (italicIdx >= 0) italicIdx else Int.MAX_VALUE

            when {
                nextBold < nextItalic && nextBold != Int.MAX_VALUE -> {
                    // Append text before **
                    ssb.append(remaining.substring(0, boldIdx))
                    remaining = remaining.substring(boldIdx + 2)
                    val end = remaining.indexOf("**")
                    if (end < 0) {
                        ssb.append("**").append(remaining)
                        remaining = ""
                    } else {
                        val boldText = remaining.substring(0, end)
                        val start = ssb.length
                        ssb.append(boldText)
                        ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        remaining = remaining.substring(end + 2)
                    }
                }
                nextItalic < Int.MAX_VALUE -> {
                    ssb.append(remaining.substring(0, italicIdx))
                    remaining = remaining.substring(italicIdx + 1)
                    val end = remaining.indexOf("*")
                    if (end < 0) {
                        ssb.append("*").append(remaining)
                        remaining = ""
                    } else {
                        val italicText = remaining.substring(0, end)
                        val start = ssb.length
                        ssb.append(italicText)
                        ssb.setSpan(StyleSpan(Typeface.ITALIC), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        remaining = remaining.substring(end + 1)
                    }
                }
                else -> {
                    ssb.append(remaining)
                    remaining = ""
                }
            }
        }
    }
}
