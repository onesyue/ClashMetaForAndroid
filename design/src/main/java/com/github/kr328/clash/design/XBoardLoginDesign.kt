package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignXboardLoginBinding
import com.github.kr328.clash.design.util.applyFrom
import com.github.kr328.clash.design.util.bindAppBarElevation
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root

class XBoardLoginDesign(context: Context) : Design<XBoardLoginDesign.Request>(context) {
    sealed class Request {
        data class Login(val url: String, val email: String, val password: String) : Request()
    }

    private val binding = DesignXboardLoginBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    var processing: Boolean
        get() = binding.processing
        set(value) {
            binding.processing = value
        }

    fun setUrlError(message: String?) {
        binding.urlLayout.error = message
    }

    fun setEmailError(message: String?) {
        binding.emailLayout.error = message
    }

    fun setPasswordError(message: String?) {
        binding.passwordLayout.error = message
    }

    fun requestLogin() {
        val url = binding.urlField.text?.toString()?.trim() ?: ""
        val email = binding.emailField.text?.toString()?.trim() ?: ""
        val password = binding.passwordField.text?.toString() ?: ""

        var hasError = false

        if (url.isBlank()) {
            binding.urlLayout.error = context.getString(R.string.should_not_be_blank)
            hasError = true
        } else {
            binding.urlLayout.error = null
        }

        if (email.isBlank()) {
            binding.emailLayout.error = context.getString(R.string.should_not_be_blank)
            hasError = true
        } else {
            binding.emailLayout.error = null
        }

        if (password.isBlank()) {
            binding.passwordLayout.error = context.getString(R.string.should_not_be_blank)
            hasError = true
        } else {
            binding.passwordLayout.error = null
        }

        if (!hasError) {
            requests.trySend(Request.Login(url, email, password))
        }
    }

    init {
        binding.self = this
        binding.processing = false

        binding.urlField.setText(context.getString(R.string.xboard_default_url))

        binding.activityBarLayout.applyFrom(context)
        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)
    }
}
