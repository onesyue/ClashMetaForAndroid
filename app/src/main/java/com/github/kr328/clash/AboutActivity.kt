package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import com.github.kr328.clash.design.AboutDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class AboutActivity : BaseActivity<AboutDesign>() {

    override suspend fun main() {
        val versionName = withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        }

        val design = AboutDesign(this, versionName)
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive { request ->
                    when (request) {
                        AboutDesign.Request.Privacy -> {
                            openUrl("https://github.com/onesyue/ClashMetaForAndroid/blob/main/PRIVACY_POLICY.md")
                        }
                        AboutDesign.Request.Feedback -> {
                            openUrl("https://github.com/onesyue/ClashMetaForAndroid/issues")
                        }
                        AboutDesign.Request.Licenses -> {
                            openUrl("https://github.com/onesyue/ClashMetaForAndroid/blob/main/LICENSE")
                        }
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
