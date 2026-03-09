package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignSettingsCommonBinding
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.preference.*
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class UserSettingsDesign(
    context: Context,
    subscriptionName: String?,
    subscriptionUpdatedAt: String?,
    versionName: String,
    private val intervalProperty: IntervalProperty,
    private val uiStore: UiStore,
) : Design<UserSettingsDesign.Request>(context) {

    enum class Request {
        UpdateSubscription,
        AddSubscription,
        CheckUpdate,
        ChangeLanguage,
        DarkModeChanged,
    }

    enum class UpdateInterval(val hours: Long) {
        Off(0), Hours12(12), Hours24(24), Hours48(48);
    }

    interface IntervalProperty {
        fun get(): UpdateInterval
        fun set(value: UpdateInterval)
    }

    /** Backing field that delegates to IntervalProperty, enabling `::autoUpdateInterval` reference. */
    var autoUpdateInterval: UpdateInterval
        get() = intervalProperty.get()
        set(value) = intervalProperty.set(value)

    private val binding = DesignSettingsCommonBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private var updateSubPref: com.github.kr328.clash.design.preference.ClickablePreference? = null

    init {
        binding.surface = surface
        binding.activityBarLayout.applyFrom(context)
        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        val screen = preferenceScreen(context) {
            // ── 订阅管理 ──
            category(R.string.cat_subscription)

            clickable(
                title = R.string.current_subscription,
                summary = R.string.no_active_subscription,
            ) {
                summary = subscriptionName
                    ?: context.getString(R.string.no_active_subscription)
            }

            updateSubPref = clickable(
                title = R.string.update_subscription,
                summary = R.string.update_subscription_summary,
            ) {
                if (subscriptionUpdatedAt != null) {
                    summary = context.getString(R.string.last_update_time, subscriptionUpdatedAt)
                }
                clicked { requests.trySend(Request.UpdateSubscription) }
            }

            clickable(
                title = R.string.add_subscription,
                summary = R.string.add_subscription_summary,
            ) {
                clicked { requests.trySend(Request.AddSubscription) }
            }

            // ── 更新设置 ──
            category(R.string.cat_update_settings)

            selectableList(
                value = ::autoUpdateInterval,
                values = UpdateInterval.values(),
                valuesText = arrayOf(
                    R.string.interval_off,
                    R.string.interval_12h,
                    R.string.interval_24h,
                    R.string.interval_48h,
                ),
                title = R.string.auto_update_interval,
            )

            // ── 应用 ──
            category(R.string.cat_app)

            selectableList(
                value = uiStore::darkMode,
                values = DarkMode.values(),
                valuesText = arrayOf(
                    R.string.follow_system_android_10,
                    R.string.always_light,
                    R.string.always_dark,
                ),
                title = R.string.dark_mode_setting,
            ) {
                listener = OnChangedListener {
                    requests.trySend(Request.DarkModeChanged)
                }
            }

            clickable(
                title = R.string.language_setting,
            ) {
                clicked { requests.trySend(Request.ChangeLanguage) }
            }

            clickable(
                title = R.string.check_update,
                summary = R.string.check_update_summary,
            ) {
                val channelName = if (versionName.contains("-alpha", ignoreCase = true))
                    context.getString(R.string.update_channel_alpha)
                else
                    context.getString(R.string.update_channel_release)
                summary = context.getString(R.string.check_update_summary, versionName, channelName)
                clicked { requests.trySend(Request.CheckUpdate) }
            }
        }

        binding.content.addView(screen.root)
    }

    fun setUpdating(updating: Boolean) {
        updateSubPref?.let {
            it.summary = if (updating) {
                context.getString(R.string.subscription_updating)
            } else {
                context.getString(R.string.update_subscription_summary)
            }
            it.enabled = !updating
        }
    }
}
