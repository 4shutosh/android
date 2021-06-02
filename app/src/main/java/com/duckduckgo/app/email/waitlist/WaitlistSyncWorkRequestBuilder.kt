/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.email.waitlist

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.duckduckgo.app.global.plugins.worker.WorkerInjectorPlugin
import com.duckduckgo.di.scopes.AppObjectGraph
import com.squareup.anvil.annotations.ContributesMultibinding
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WaitlistSyncWorkRequestBuilder @Inject constructor() {

    fun appConfigurationWork(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<EmailWaitlistWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkAvailable())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .setInitialDelay(5, TimeUnit.SECONDS)
            .addTag(EMAIL_WAITLIST_SYNC_WORK_TAG)
            .build()
    }

    private fun networkAvailable() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    companion object {
        const val EMAIL_WAITLIST_SYNC_WORK_TAG = "EmailWaitlistWorker"
    }
}

class EmailWaitlistWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var waitlistCodeFetcher: WaitlistCodeFetcher

    override suspend fun doWork(): Result {
        return waitlistCodeFetcher.fetchInviteCode()
    }
}

@ContributesMultibinding(AppObjectGraph::class)
class AppConfigurationWorkerInjectorPlugin @Inject constructor(
    private val waitlistCodeFetcher: WaitlistCodeFetcher
) : WorkerInjectorPlugin {

    override fun inject(worker: ListenableWorker): Boolean {
        if (worker is EmailWaitlistWorker) {
            worker.waitlistCodeFetcher = waitlistCodeFetcher
            return true
        }
        return false
    }
}