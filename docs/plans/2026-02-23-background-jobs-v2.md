# Background Jobs (V2) Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Add a dedicated Background jobs screen (Synapse background updates status, pause/resume, start job) entered from the More tab.

**Architecture:** New `:feature:jobs` module with data layer (JobsApi, models, JobsRepository) and UI (JobsViewModel, BackgroundJobsScreen). Route `BackgroundJobs(serverId, serverUrl)`; MoreScreen gets an "Background jobs" row that navigates to it. No audit logging in this version.

**Tech Stack:** Kotlin 2.0, Compose Material3, Hilt/KSP, Retrofit, kotlinx-serialization, mockk, turbine, JUnit4

---

## API Reference

- `GET /_synapse/admin/v1/background_updates/status` → `{ "enabled": boolean, "current_updates": { "<db_name>": { "name", "total_item_count", "total_duration_ms", "average_items_per_ms" } } }`
- `GET /_synapse/admin/v1/background_updates/enabled` → `{ "enabled": boolean }`
- `POST /_synapse/admin/v1/background_updates/enabled` body `{ "enabled": boolean }` → `{ "enabled": boolean }`
- `POST /_synapse/admin/v1/background_updates/start_job` body `{ "job_name": "regenerate_directory" }` (or `populate_stats_process_rooms`)

---

### Task 1: Scaffold `:feature:jobs` module

**Files:**
- Create: `feature/jobs/build.gradle.kts`
- Create: `feature/jobs/src/main/AndroidManifest.xml`
- Create: `feature/jobs/consumer-rules.pro` (empty)
- Create: `feature/jobs/proguard-rules.pro` (empty)
- Modify: `settings.gradle.kts` — add `include(":feature:jobs")`
- Modify: `app/build.gradle.kts` — add `implementation(project(":feature:jobs"))`

**Step 1: Create build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.matrix.synapse.feature.jobs"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:network"))
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.navigation.compose)
    implementation(libs.bundles.lifecycle)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)
    testImplementation(libs.bundles.unit.test)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

**Step 2: Create AndroidManifest.xml**

File: `feature/jobs/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

**Step 3: Create empty consumer-rules.pro and proguard-rules.pro**

Create empty files at `feature/jobs/consumer-rules.pro` and `feature/jobs/proguard-rules.pro`.

**Step 4: Add to settings.gradle.kts**

Add after `include(":feature:federation")`:

```kotlin
include(":feature:jobs")
```

**Step 5: Add to app/build.gradle.kts**

Add after `implementation(project(":feature:federation"))`:

```kotlin
implementation(project(":feature:jobs"))
```

**Step 6: Sync and compile**

Run: `./gradlew :feature:jobs:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (module has no sources yet but resolves).

**Step 7: Commit**

```bash
git add feature/jobs/build.gradle.kts feature/jobs/src/main/AndroidManifest.xml \
       feature/jobs/consumer-rules.pro feature/jobs/proguard-rules.pro \
       settings.gradle.kts app/build.gradle.kts
git commit -m "chore: scaffold :feature:jobs module"
```

---

### Task 2: Create jobs data layer (models, API, repository)

**Files:**
- Create: `feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/data/JobsModels.kt`
- Create: `feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/data/JobsApi.kt`
- Create: `feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/data/JobsRepository.kt`

**Step 1: Create JobsModels.kt**

```kotlin
package com.matrix.synapse.feature.jobs.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackgroundUpdatesStatusResponse(
    val enabled: Boolean = true,
    @SerialName("current_updates") val currentUpdates: Map<String, CurrentUpdateInfo> = emptyMap(),
)

@Serializable
data class CurrentUpdateInfo(
    val name: String = "",
    @SerialName("total_item_count") val totalItemCount: Long = 0L,
    @SerialName("total_duration_ms") val totalDurationMs: Double = 0.0,
    @SerialName("average_items_per_ms") val averageItemsPerMs: Double = 0.0,
)

@Serializable
data class EnabledResponse(val enabled: Boolean)

@Serializable
data class EnabledRequest(val enabled: Boolean)

@Serializable
data class StartJobRequest(@SerialName("job_name") val jobName: String)
```

**Step 2: Create JobsApi.kt**

```kotlin
package com.matrix.synapse.feature.jobs.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface JobsApi {
    @GET("/_synapse/admin/v1/background_updates/status")
    suspend fun getStatus(): BackgroundUpdatesStatusResponse

    @GET("/_synapse/admin/v1/background_updates/enabled")
    suspend fun getEnabled(): EnabledResponse

    @POST("/_synapse/admin/v1/background_updates/enabled")
    suspend fun setEnabled(@Body body: EnabledRequest): EnabledResponse

    @POST("/_synapse/admin/v1/background_updates/start_job")
    suspend fun startJob(@Body body: StartJobRequest)
}
```

**Step 3: Create JobsRepository.kt**

```kotlin
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
```

**Step 4: Compile check**

Run: `./gradlew :feature:jobs:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/data/
git commit -m "feat: add jobs data layer — API, models, repository"
```

---

### Task 3: Create JobsViewModel

**Files:**
- Create: `feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/ui/JobsViewModel.kt`

**Step 1: Create JobsViewModel.kt**

```kotlin
package com.matrix.synapse.feature.jobs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.jobs.data.CurrentUpdateInfo
import com.matrix.synapse.feature.jobs.data.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackgroundJobsState(
    val enabled: Boolean? = null,
    val currentUpdates: Map<String, CurrentUpdateInfo> = emptyMap(),
    val isLoading: Boolean = false,
    val isToggling: Boolean = false,
    val isStartingJob: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackgroundJobsState())
    val state: StateFlow<BackgroundJobsState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun load(serverId: String, serverUrl: String) {
        this.serverUrl = serverUrl
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { jobsRepository.getStatus(serverUrl) }
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        enabled = response.enabled,
                        currentUpdates = response.currentUpdates,
                        isLoading = false,
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message ?: "Failed to load status",
                        isLoading = false,
                    )
                }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isToggling = true, error = null)
        viewModelScope.launch {
            runCatching { jobsRepository.setEnabled(serverUrl, enabled) }
                .onSuccess { newEnabled ->
                    _state.value = _state.value.copy(
                        enabled = newEnabled,
                        isToggling = false,
                        successMessage = if (newEnabled) "Background updates resumed" else "Background updates paused",
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isToggling = false,
                        error = e.message ?: "Failed to update",
                    )
                }
        }
    }

    fun startJob(jobName: String) {
        _state.value = _state.value.copy(isStartingJob = true, error = null, successMessage = null)
        viewModelScope.launch {
            runCatching { jobsRepository.startJob(serverUrl, jobName) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isStartingJob = false,
                        successMessage = "Job started: $jobName",
                    )
                    load("", serverUrl)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isStartingJob = false,
                        error = e.message ?: "Failed to start job",
                    )
                }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }
}
```

**Step 2: Compile check**

Run: `./gradlew :feature:jobs:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/ui/JobsViewModel.kt
git commit -m "feat: add JobsViewModel"
```

---

### Task 4: Write JobsViewModel unit tests

**Files:**
- Create: `feature/jobs/src/test/kotlin/com/matrix/synapse/feature/jobs/ui/JobsViewModelTest.kt`

**Step 1: Create JobsViewModelTest.kt**

```kotlin
package com.matrix.synapse.feature.jobs.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.jobs.data.BackgroundUpdatesStatusResponse
import com.matrix.synapse.feature.jobs.data.CurrentUpdateInfo
import com.matrix.synapse.feature.jobs.data.JobsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

    private val jobsRepository = mockk<JobsRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun load_success_populates_enabled_and_currentUpdates() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(
            enabled = true,
            currentUpdates = mapOf(
                "master" to CurrentUpdateInfo(
                    name = "populate_stats_process_rooms",
                    totalItemCount = 100L,
                    totalDurationMs = 5000.0,
                    averageItemsPerMs = 0.02,
                ),
            ),
        )

        val vm = JobsViewModel(jobsRepository)
        vm.state.test {
            vm.load("srv1", "https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(true, state.enabled)
            assertEquals(1, state.currentUpdates.size)
            assertEquals("populate_stats_process_rooms", state.currentUpdates["master"]?.name)
        }
    }

    @Test
    fun load_failure_sets_error() = runTest {
        coEvery { jobsRepository.getStatus(any()) } throws RuntimeException("network error")

        val vm = JobsViewModel(jobsRepository)
        vm.state.test {
            vm.load("srv1", "https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
        }
    }

    @Test
    fun setEnabled_success_updates_state() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { jobsRepository.setEnabled(any(), any()) } returns false

        val vm = JobsViewModel(jobsRepository)
        vm.load("srv1", "https://example.com")
        vm.state.test {
            vm.setEnabled(false)
            val state = expectMostRecentItem()
            assertFalse(state.isToggling)
            assertEquals(false, state.enabled)
        }
    }

    @Test
    fun startJob_success_refreshes_and_shows_message() = runTest {
        coEvery { jobsRepository.getStatus(any()) } returns BackgroundUpdatesStatusResponse(enabled = true)
        coEvery { jobsRepository.startJob(any(), any()) } returns Unit

        val vm = JobsViewModel(jobsRepository)
        vm.load("srv1", "https://example.com")
        vm.state.test {
            vm.startJob("regenerate_directory")
            val state = expectMostRecentItem()
            assertFalse(state.isStartingJob)
            assertTrue(state.successMessage?.contains("regenerate_directory") == true)
        }
    }
}
```

**Step 2: Run tests**

Run: `./gradlew :feature:jobs:testDebugUnitTest`
Expected: All tests PASSED

**Step 3: Commit**

```bash
git add feature/jobs/src/test/kotlin/com/matrix/synapse/feature/jobs/ui/JobsViewModelTest.kt
git commit -m "test: add JobsViewModel unit tests"
```

---

### Task 5: Create BackgroundJobsScreen

**Files:**
- Create: `feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/ui/BackgroundJobsScreen.kt`

**Step 1: Create BackgroundJobsScreen.kt**

```kotlin
package com.matrix.synapse.feature.jobs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val JOB_NAMES = listOf(
    "regenerate_directory" to "Regenerate user directory",
    "populate_stats_process_rooms" to "Recalculate room stats",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundJobsScreen(
    serverId: String,
    serverUrl: String,
    onBack: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverId, serverUrl) { viewModel.load(serverId, serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Background jobs") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading && state.enabled == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.enabled == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { viewModel.load(serverId, serverUrl) }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.enabled?.let { enabled ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Background updates", style = MaterialTheme.typography.titleMedium)
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { viewModel.setEnabled(it) },
                                    enabled = !state.isToggling,
                                )
                            }
                        }
                    }

                    if (state.currentUpdates.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Current updates", style = MaterialTheme.typography.titleMedium)
                                state.currentUpdates.forEach { (dbName, info) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(info.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(dbName, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${info.totalItemCount} items", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "${info.totalDurationMs.toLong()} ms",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Run job", style = MaterialTheme.typography.titleMedium)
                            JOB_NAMES.forEach { (name, label) ->
                                Button(
                                    onClick = { viewModel.startJob(name) },
                                    enabled = !state.isStartingJob,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

**Step 2: Compile check**

Run: `./gradlew :feature:jobs:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add feature/jobs/src/main/kotlin/com/matrix/synapse/feature/jobs/ui/BackgroundJobsScreen.kt
git commit -m "feat: add BackgroundJobsScreen"
```

---

### Task 6: Wire route, MoreScreen, and AppNavHost

**Files:**
- Modify: `app/src/main/kotlin/com/matrix/synapse/manager/AppRoutes.kt`
- Modify: `app/src/main/kotlin/com/matrix/synapse/manager/MoreScreen.kt`
- Modify: `app/src/main/kotlin/com/matrix/synapse/manager/AppNavHost.kt`

**Step 1: Add route to AppRoutes.kt**

Add after `data class FederationDetail(...)`:

```kotlin
@Serializable
data class BackgroundJobs(val serverId: String, val serverUrl: String)
```

**Step 2: Add Background jobs row and callback to MoreScreen.kt**

Add parameter to `MoreScreen` (after `onFederation`):

```kotlin
onBackgroundJobs: () -> Unit = {},
```

Add a new list row and divider **between** Federation and Audit logs (after the Federation `ListItem` and its `HorizontalDivider`, add):

```kotlin
            ListItem(
                headlineContent = { Text("Background jobs") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBackgroundJobs() },
            )
            HorizontalDivider()
```

**Step 3: Wire composable and callback in AppNavHost.kt**

Add import:

```kotlin
import com.matrix.synapse.feature.jobs.ui.BackgroundJobsScreen
```

In the `composable<More>` block, add `onBackgroundJobs` to the `MoreScreen` call:

```kotlin
                onBackgroundJobs = {
                    navController.navigate(BackgroundJobs(route.serverId, route.serverUrl))
                },
```

Add new composable after `composable<FederationDetail>` (before the closing `}` of the NavHost):

```kotlin
        composable<BackgroundJobs> { backStack ->
            val route = backStack.toRoute<BackgroundJobs>()
            BackgroundJobsScreen(
                serverId = route.serverId,
                serverUrl = route.serverUrl,
                onBack = { navController.popBackStack() },
            )
        }
```

**Step 4: Full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add app/src/main/kotlin/com/matrix/synapse/manager/AppRoutes.kt \
       app/src/main/kotlin/com/matrix/synapse/manager/MoreScreen.kt \
       app/src/main/kotlin/com/matrix/synapse/manager/AppNavHost.kt
git commit -m "feat: wire Background jobs route and More tab entry"
```

---

### Task 7: Update compatibility matrix (optional)

**Files:**
- Modify: `docs/compatibility-matrix.md`

Add a row under the Feature / Endpoint Matrix:

| Background updates status | `GET /_synapse/admin/v1/background_updates/status` | (check Synapse docs) | View and control background jobs |
| Background updates enabled | `GET|POST /_synapse/admin/v1/background_updates/enabled` | (same) | Pause/resume |
| Start background job | `POST /_synapse/admin/v1/background_updates/start_job` | (same) | Run specific job |

Fill in minimum Synapse version after verifying against Synapse docs or a test server.

---

## Summary

| Task | Description |
|------|-------------|
| 1 | Scaffold :feature:jobs module |
| 2 | Data layer: JobsModels, JobsApi, JobsRepository |
| 3 | JobsViewModel |
| 4 | JobsViewModel unit tests |
| 5 | BackgroundJobsScreen (Run job = two buttons) |
| 6 | Route, MoreScreen row, AppNavHost composable |
| 7 | Optional: compatibility matrix |

## Definition of Done

- More tab shows "Background jobs"; tapping opens Background jobs screen.
- Screen shows enabled toggle, current updates list (when any), and Run job buttons (regenerate_directory, populate_stats_process_rooms).
- Pause/resume and start job work; errors and success messages show via snackbar.
- Unit tests for JobsViewModel pass.
- `./gradlew assembleDebug` and `testDebugUnitTest` pass.
