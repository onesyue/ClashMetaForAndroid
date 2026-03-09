package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.kr328.clash.design.UserSettingsDesign
import com.github.kr328.clash.design.UserSettingsDesign.UpdateInterval
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.Language
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.ApplicationObserver
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class UserSettingsActivity : BaseActivity<UserSettingsDesign>() {

    override suspend fun main() {
        val activeProfile = withProfile { queryActive() }
        val subscriptionName = activeProfile?.name
        val subscriptionUpdatedAt = activeProfile?.updatedAt?.let { ts ->
            if (ts > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(ts))
            else null
        }
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"

        val intervalProperty = object : UserSettingsDesign.IntervalProperty {
            override fun get(): UpdateInterval {
                val millis = activeProfile?.interval ?: 0L
                val hours = TimeUnit.MILLISECONDS.toHours(millis)
                return when {
                    hours >= 48 -> UpdateInterval.Hours48
                    hours >= 24 -> UpdateInterval.Hours24
                    hours >= 12 -> UpdateInterval.Hours12
                    else -> UpdateInterval.Off
                }
            }

            override fun set(value: UpdateInterval) {
                val profile = activeProfile ?: return
                val intervalMs = TimeUnit.HOURS.toMillis(value.hours)
                launch(Dispatchers.IO) {
                    withProfile {
                        patch(profile.uuid, profile.name, profile.source, intervalMs)
                        commit(profile.uuid, null)
                    }
                }
            }
        }

        val design = UserSettingsDesign(
            this, subscriptionName, subscriptionUpdatedAt, versionName, intervalProperty, uiStore
        )
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ProfileChanged -> {}
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        UserSettingsDesign.Request.UpdateSubscription ->
                            handleUpdateSubscription(design)
                        UserSettingsDesign.Request.AddSubscription ->
                            handleAddSubscription(design)
                        UserSettingsDesign.Request.CheckUpdate ->
                            handleCheckUpdate(design, versionName)
                        UserSettingsDesign.Request.ChangeLanguage ->
                            handleChangeLanguage()
                        UserSettingsDesign.Request.DarkModeChanged -> {
                            ApplicationObserver.createdActivities.forEach { it.recreate() }
                        }
                    }
                }
            }
        }
    }

    private fun handleUpdateSubscription(design: UserSettingsDesign) {
        launch(Dispatchers.IO) {
            val active = withProfile { queryActive() }
            if (active == null) {
                design.showToast(
                    getString(R.string.no_active_subscription), ToastDuration.Long
                )
                return@launch
            }
            withContext(Dispatchers.Main) { design.setUpdating(true) }
            try {
                withProfile { update(active.uuid) }
                design.showToast(
                    getString(R.string.subscription_update_success), ToastDuration.Short
                )
            } catch (e: Exception) {
                design.showToast(
                    getString(R.string.subscription_update_failed, e.message ?: ""),
                    ToastDuration.Long
                )
            } finally {
                withContext(Dispatchers.Main) { design.setUpdating(false) }
            }
        }
    }

    private suspend fun handleAddSubscription(design: UserSettingsDesign) {
        withContext(Dispatchers.Main) {
            val dp = (resources.displayMetrics.density + 0.5f).toInt()
            val pad = 20 * dp
            val urlEdit = EditText(this@UserSettingsActivity).apply {
                hint = getString(R.string.subscription_url_hint)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_URI
                isSingleLine = true
            }
            val layout = LinearLayout(this@UserSettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, 0, pad, 0)
                addView(urlEdit, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))
            }
            AlertDialog.Builder(this@UserSettingsActivity)
                .setTitle(R.string.add_subscription)
                .setMessage(R.string.enter_subscription_url)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val url = urlEdit.text?.toString()?.trim() ?: ""
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        launch(Dispatchers.IO) {
                            val brandName = getString(R.string.xboard_brand_name)
                            withProfile {
                                queryAll().forEach { delete(it.uuid) }
                                create(Profile.Type.Url, brandName, url)
                            }
                            design.showToast(
                                getString(R.string.subscription_added), ToastDuration.Long
                            )
                        }
                    } else {
                        launch {
                            design.showToast(
                                getString(R.string.invalid_subscription_url), ToastDuration.Long
                            )
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun handleCheckUpdate(design: UserSettingsDesign, currentVersion: String) {
        launch(Dispatchers.IO) {
            design.showToast(getString(R.string.checking_update), ToastDuration.Short)
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/onesyue/ClashMetaForAndroid/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        design.showToast(getString(R.string.check_update_failed), ToastDuration.Long)
                        return@launch
                    }
                    val body = response.body?.string() ?: return@launch
                    val json = org.json.JSONObject(body)
                    val latestTag = json.optString("tag_name", "").removePrefix("v")
                    val htmlUrl = json.optString("html_url", "")

                    if (latestTag.isNotEmpty() && latestTag != currentVersion &&
                        isNewerVersion(latestTag, currentVersion)
                    ) {
                        withContext(Dispatchers.Main) {
                            AlertDialog.Builder(this@UserSettingsActivity)
                                .setTitle(R.string.check_update)
                                .setMessage(getString(R.string.update_available, latestTag))
                                .setPositiveButton(R.string.update_download) { _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl))
                                    startActivity(intent)
                                }
                                .setNegativeButton(R.string.cancel, null)
                                .show()
                        }
                    } else {
                        design.showToast(getString(R.string.already_latest), ToastDuration.Short)
                    }
                }
            } catch (e: Exception) {
                design.showToast(getString(R.string.check_update_failed), ToastDuration.Long)
            }
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    private fun handleChangeLanguage() {
        val languages = Language.values()
        val languageNames = arrayOf(
            getString(R.string.lang_system),
            getString(R.string.lang_english),
            getString(R.string.lang_chinese_simplified),
            getString(R.string.lang_chinese_traditional),
            getString(R.string.lang_chinese_hk),
            getString(R.string.lang_japanese),
            getString(R.string.lang_korean),
            getString(R.string.lang_vietnamese),
            getString(R.string.lang_russian),
        )
        val currentIdx = languages.indexOf(uiStore.language).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.language_setting)
            .setSingleChoiceItems(languageNames, currentIdx) { dialog, which ->
                dialog.dismiss()
                uiStore.language = languages[which]
                applyLanguage(languages[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applyLanguage(language: Language) {
        val tag = when (language) {
            Language.System -> ""
            Language.English -> "en"
            Language.ChineseSimplified -> "zh"
            Language.ChineseTraditional -> "zh-TW"
            Language.ChineseHK -> "zh-HK"
            Language.Japanese -> "ja-JP"
            Language.Korean -> "ko-KR"
            Language.Vietnamese -> "vi"
            Language.Russian -> "ru"
        }
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
