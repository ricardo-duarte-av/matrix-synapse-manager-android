package com.matrix.synapse.feature.jobs.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobsRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): JobsApi = retrofitFactory.create(serverUrl)

    suspend fun getStatus(serverUrl: String): BackgroundUpdatesStatusResponse =
        api(serverUrl).getStatus()

    suspend fun getEnabled(serverUrl: String): Boolean =
        api(serverUrl).getEnabled().enabled

    suspend fun setEnabled(serverUrl: String, enabled: Boolean): Boolean =
        api(serverUrl).setEnabled(EnabledRequest(enabled)).enabled

    suspend fun startJob(serverUrl: String, jobName: String) {
        api(serverUrl).startJob(StartJobRequest(jobName))
    }
}
