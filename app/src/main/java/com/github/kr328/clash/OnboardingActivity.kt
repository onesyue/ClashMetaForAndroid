package com.github.kr328.clash

import com.github.kr328.clash.design.OnboardingDesign
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class OnboardingActivity : BaseActivity<OnboardingDesign>() {

    override suspend fun main() {
        val design = OnboardingDesign(this)
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive { request ->
                    when (request) {
                        OnboardingDesign.Request.Skip,
                        OnboardingDesign.Request.Start -> {
                            markOnboardingComplete()
                            setResult(RESULT_OK)
                            finish()
                        }
                        OnboardingDesign.Request.Next -> { /* handled by Design */ }
                    }
                }
            }
        }
    }

    private fun markOnboardingComplete() {
        getSharedPreferences("yt_app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", true)
            .apply()
    }

    override fun shouldDisplayHomeAsUpEnabled(): Boolean = false
}
