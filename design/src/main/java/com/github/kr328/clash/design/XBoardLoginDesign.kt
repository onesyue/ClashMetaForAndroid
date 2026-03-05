package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignXboardLoginBinding
import com.github.kr328.clash.design.util.*
import com.google.android.material.tabs.TabLayout

class XBoardLoginDesign(context: Context) : Design<XBoardLoginDesign.Request>(context) {

    sealed class Request {
        data class Login(
            val url: String,
            val email: String,
            val password: String
        ) : Request()

        data class Register(
            val url: String,
            val email: String,
            val password: String,
            val inviteCode: String
        ) : Request()
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

    fun requestLogin() {
        val url = binding.urlField.text?.toString()?.trim() ?: ""
        val email = binding.loginEmailField.text?.toString()?.trim() ?: ""
        val password = binding.loginPasswordField.text?.toString() ?: ""

        if (!validateFields(
                urlPair = url to binding.urlLayout,
                emailPair = email to binding.loginEmailLayout,
                passwordPair = password to binding.loginPasswordLayout
            )
        ) return

        requests.trySend(Request.Login(url, email, password))
    }

    fun requestRegister() {
        val url = binding.urlField.text?.toString()?.trim() ?: ""
        val email = binding.registerEmailField.text?.toString()?.trim() ?: ""
        val password = binding.registerPasswordField.text?.toString() ?: ""
        val inviteCode = binding.registerInviteField.text?.toString()?.trim() ?: ""

        if (!validateFields(
                urlPair = url to binding.urlLayout,
                emailPair = email to binding.registerEmailLayout,
                passwordPair = password to binding.registerPasswordLayout
            )
        ) return

        requests.trySend(Request.Register(url, email, password, inviteCode))
    }

    private fun validateFields(
        urlPair: Pair<String, com.google.android.material.textfield.TextInputLayout>,
        emailPair: Pair<String, com.google.android.material.textfield.TextInputLayout>,
        passwordPair: Pair<String, com.google.android.material.textfield.TextInputLayout>
    ): Boolean {
        val blank = context.getString(R.string.should_not_be_blank)
        var ok = true

        if (urlPair.first.isBlank()) {
            urlPair.second.error = blank; ok = false
        } else urlPair.second.error = null

        if (emailPair.first.isBlank()) {
            emailPair.second.error = blank; ok = false
        } else emailPair.second.error = null

        if (passwordPair.first.isBlank()) {
            passwordPair.second.error = blank; ok = false
        } else passwordPair.second.error = null

        return ok
    }

    fun showError(message: String) {
        binding.urlLayout.error = message
    }

    init {
        binding.self = this
        binding.processing = false

        binding.urlField.setText(context.getString(R.string.xboard_default_url))

        binding.activityBarLayout.applyFrom(context)
        binding.scrollRoot.bindAppBarElevation(binding.activityBarLayout)

        // 初始化 Tab
        binding.tabLayout.apply {
            addTab(newTab().setText(context.getString(R.string.xboard_tab_login)))
            addTab(newTab().setText(context.getString(R.string.xboard_tab_register)))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            binding.loginForm.visibility = View.VISIBLE
                            binding.registerForm.visibility = View.GONE
                        }
                        else -> {
                            binding.loginForm.visibility = View.GONE
                            binding.registerForm.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }
}
