package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class AboutDesign(context: Context, versionName: String) : Design<AboutDesign.Request>(context) {

    enum class Request { Privacy, Feedback, Licenses }

    private val binding = DesignAboutBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.activityBarLayout.applyFrom(context)
        binding.aboutVersionText.text = context.getString(R.string.about_version, versionName)
        binding.aboutPrivacyBtn.setOnClickListener { requests.trySend(Request.Privacy) }
        binding.aboutFeedbackBtn.setOnClickListener { requests.trySend(Request.Feedback) }
        binding.aboutLicensesBtn.setOnClickListener { requests.trySend(Request.Licenses) }
    }
}
