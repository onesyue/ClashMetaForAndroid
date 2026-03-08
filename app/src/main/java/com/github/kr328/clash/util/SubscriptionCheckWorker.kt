package com.github.kr328.clash.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.kr328.clash.xboard.XBoardApi
import com.github.kr328.clash.xboard.XBoardSession
import java.util.concurrent.TimeUnit

class SubscriptionCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val authData = XBoardSession.getAuthData(applicationContext) ?: return Result.success()
        val baseUrl = XBoardSession.getBaseUrl(applicationContext)

        return try {
            val info = XBoardApi.getUserInfo(baseUrl, authData) ?: return Result.success()

            // Cache fresh data
            OfflineCache.put(
                applicationContext, OfflineCache.KEY_USER_INFO, info.toJson()
            )

            // Check expiry and notify
            SubscriptionChecker.check(applicationContext, info.expiredAt)

            Result.success()
        } catch (_: XBoardApi.AuthExpiredException) {
            Result.success() // Don't retry on auth failure
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "yt_subscription_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(
                12, TimeUnit.HOURS,
                2, TimeUnit.HOURS // flex interval
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
