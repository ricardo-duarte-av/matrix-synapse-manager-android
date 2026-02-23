# Deactivate Button, Media Management, Federation — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Wire the existing deactivate button into user detail UI, add a full media admin module, and add a federation monitoring module.

**Architecture:** Three independent feature areas following existing module-per-feature pattern. Deactivate modifies `:feature:users` only. Media and Federation each get a new Gradle module (`:feature:media`, `:feature:federation`) with API/Repository/ViewModel/Screen layers, wired into `:app` nav graph. All follow existing TDD + mockk/turbine patterns.

**Tech Stack:** Kotlin 2.0, Compose Material3, Hilt/KSP, Retrofit, kotlinx-serialization, mockk, turbine, JUnit4

---

## Part 1: Deactivate Button

Existing code: `DeactivateUserUseCase`, `DeactivateDialogState`, `DeactivateRequest/Response`, `UserAdminApi.deactivateUser()`, `UserRepository.deactivateUser()`. Missing: ViewModel wiring and Screen UI.

### Task 1: Add deactivate state and method to UserDetailViewModel

**Files:**
- Modify: `feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailViewModel.kt`

**Step 1: Add state fields to `UserDetailState`**

Add `isDeactivating` and `isDeactivated` fields:

```kotlin
data class UserDetailState(
    val user: UserDetail? = null,
    val isLoading: Boolean = false,
    val isLocking: Boolean = false,
    val isSuspending: Boolean = false,
    val canSuspend: Boolean = false,
    val isDeactivating: Boolean = false,
    val isDeactivated: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)
```

**Step 2: Add DeactivateUserUseCase and AuditLogger dependencies**

Update the constructor to inject `DeactivateUserUseCase` and `AuditLogger`:

```kotlin
@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val capabilityService: CapabilityService,
    private val deactivateUserUseCase: DeactivateUserUseCase,
    private val auditLogger: AuditLogger,
) : ViewModel() {
```

Add imports:
```kotlin
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
```

**Step 3: Add `deactivateUser` method**

Add after `setSuspended()`:

```kotlin
fun deactivateUser(serverUrl: String, serverId: String, userId: String, deleteMedia: Boolean) {
    _state.value = _state.value.copy(isDeactivating = true, error = null)
    viewModelScope.launch {
        deactivateUserUseCase.deactivate(
            serverUrl = serverUrl,
            userId = userId,
            deleteMedia = deleteMedia,
            confirmed = true,
        ).onSuccess {
            _state.value = _state.value.copy(
                isDeactivating = false,
                isDeactivated = true,
                successMessage = "User deactivated",
            )
            auditLogger.insert(
                AuditLogEntry(
                    serverId = serverId,
                    action = AuditAction.DEACTIVATE_USER,
                    targetUserId = userId,
                    details = mapOf("erase" to deleteMedia.toString()),
                )
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(isDeactivating = false, error = e.message)
        }
    }
}
```

**Step 4: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:users:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

### Task 2: Write UserDetailViewModel deactivate test

**Files:**
- Create: `feature/users/src/test/kotlin/com/matrix/synapse/feature/users/ui/UserDetailViewModelDeactivateTest.kt`

**Step 1: Write the test file**

```kotlin
package com.matrix.synapse.feature.users.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.users.data.UserDetail
import com.matrix.synapse.feature.users.data.UserRepository
import com.matrix.synapse.feature.users.domain.DeactivateUserUseCase
import com.matrix.synapse.network.CapabilityService
import com.matrix.synapse.model.ServerCapabilities
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelDeactivateTest {

    private val userRepository = mockk<UserRepository>()
    private val capabilityService = mockk<CapabilityService>()
    private val deactivateUserUseCase = mockk<DeactivateUserUseCase>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = UserDetailViewModel(
        userRepository, capabilityService, deactivateUserUseCase, auditLogger,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `deactivateUser sets isDeactivated on success`() = runTest {
        coEvery {
            deactivateUserUseCase.deactivate(any(), any(), any(), any())
        } returns Result.success(Unit)

        val vm = createVm()
        vm.state.test {
            vm.deactivateUser("https://x", "srv1", "@user:x", deleteMedia = false)
            val state = expectMostRecentItem()
            assertTrue(state.isDeactivated)
            assertFalse(state.isDeactivating)
            assertEquals("User deactivated", state.successMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.DEACTIVATE_USER }) }
        }
    }

    @Test
    fun `deactivateUser sets error on failure`() = runTest {
        coEvery {
            deactivateUserUseCase.deactivate(any(), any(), any(), any())
        } returns Result.failure(RuntimeException("deactivation failed"))

        val vm = createVm()
        vm.state.test {
            vm.deactivateUser("https://x", "srv1", "@user:x", deleteMedia = true)
            val state = expectMostRecentItem()
            assertFalse(state.isDeactivated)
            assertFalse(state.isDeactivating)
            assertNotNull(state.error)
        }
    }
}
```

**Step 2: Run tests**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:users:testDebugUnitTest --tests "com.matrix.synapse.feature.users.ui.UserDetailViewModelDeactivateTest"`
Expected: 2 tests PASSED

**Step 3: Commit**

```bash
git add feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailViewModel.kt \
       feature/users/src/test/kotlin/com/matrix/synapse/feature/users/ui/UserDetailViewModelDeactivateTest.kt
git commit -m "feat: add deactivate method to UserDetailViewModel with tests"
```

### Task 3: Add deactivate button and dialog to UserDetailScreen

**Files:**
- Modify: `feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailScreen.kt`

**Step 1: Add imports and dialog state**

Add these imports at the top:
```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
```

Add dialog state inside the composable, after the existing `remember` calls:
```kotlin
var showDeactivateDialog by remember { mutableStateOf(false) }
var deleteMediaChecked by remember { mutableStateOf(false) }
```

**Step 2: Add the deactivate button after the Whois button**

After the `OutlinedButton(onClick = onWhois, ...)` block, add:

```kotlin
HorizontalDivider()
Spacer(Modifier.height(4.dp))

if (state.isDeactivated || user.deactivated) {
    Text(
        "User Deactivated",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelLarge,
    )
} else {
    OutlinedButton(
        onClick = { showDeactivateDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        enabled = !state.isDeactivating,
    ) {
        if (state.isDeactivating) {
            CircularProgressIndicator(modifier = Modifier.height(18.dp))
        } else {
            Text("Deactivate User")
        }
    }
}
```

**Step 3: Add the confirmation dialog**

After the closing `Scaffold` brace, before the function's final `}`, add:

```kotlin
if (showDeactivateDialog) {
    AlertDialog(
        onDismissRequest = { showDeactivateDialog = false },
        title = { Text("Deactivate User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This action is irreversible. The user will be permanently deactivated.")
                Text(userId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteMediaChecked,
                        onCheckedChange = { deleteMediaChecked = it },
                    )
                    Text("Delete all user media")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showDeactivateDialog = false
                    viewModel.deactivateUser(serverUrl, serverId, userId, deleteMediaChecked)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Deactivate") }
        },
        dismissButton = {
            OutlinedButton(onClick = { showDeactivateDialog = false }) { Text("Cancel") }
        },
    )
}
```

**Step 4: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:users:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailScreen.kt
git commit -m "feat: add deactivate button and confirmation dialog to UserDetailScreen"
```

---

## Part 2: Media Management Module

### Task 4: Scaffold `:feature:media` module

**Files:**
- Create: `feature/media/build.gradle.kts`
- Create: `feature/media/src/main/AndroidManifest.xml`
- Create: `feature/media/consumer-rules.pro` (empty)
- Create: `feature/media/proguard-rules.pro` (empty)
- Modify: `settings.gradle.kts` — add `include(":feature:media")`
- Modify: `app/build.gradle.kts` — add `implementation(project(":feature:media"))`

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
    namespace = "com.matrix.synapse.feature.media"
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))

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
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

**Step 2: Create AndroidManifest.xml**

File: `feature/media/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

**Step 3: Create empty proguard files**

Create empty `feature/media/consumer-rules.pro` and `feature/media/proguard-rules.pro`.

**Step 4: Add to settings.gradle.kts**

Add after `include(":feature:stats")`:
```kotlin
include(":feature:media")
```

**Step 5: Add to app/build.gradle.kts**

Add after `implementation(project(":feature:stats"))`:
```kotlin
implementation(project(":feature:media"))
```

**Step 6: Sync check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:media:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no sources yet, but module resolves)

**Step 7: Commit**

```bash
git add feature/media/build.gradle.kts feature/media/src/main/AndroidManifest.xml \
       feature/media/consumer-rules.pro feature/media/proguard-rules.pro \
       settings.gradle.kts app/build.gradle.kts
git commit -m "feat: scaffold :feature:media module"
```

### Task 5: Add media audit actions

**Files:**
- Modify: `core/database/src/main/kotlin/com/matrix/synapse/database/AuditAction.kt`

**Step 1: Add new actions**

Add after `MAKE_ROOM_ADMIN,`:
```kotlin
QUARANTINE_MEDIA,
UNQUARANTINE_MEDIA,
PROTECT_MEDIA,
UNPROTECT_MEDIA,
DELETE_MEDIA,
BULK_DELETE_MEDIA,
PURGE_REMOTE_MEDIA_CACHE,
```

**Step 2: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :core:database:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add core/database/src/main/kotlin/com/matrix/synapse/database/AuditAction.kt
git commit -m "feat: add media audit actions"
```

### Task 6: Create media data layer (API, models, repository)

**Files:**
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/data/MediaModels.kt`
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/data/MediaAdminApi.kt`
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/data/MediaRepository.kt`

**Step 1: Create MediaModels.kt**

```kotlin
package com.matrix.synapse.feature.media.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomMediaResponse(
    val local: List<String> = emptyList(),
    val remote: List<String> = emptyList(),
)

@Serializable
data class MediaInfo(
    @SerialName("media_id") val mediaId: String,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_length") val mediaLength: Long = 0L,
    @SerialName("upload_name") val uploadName: String? = null,
    @SerialName("created_ts") val createdTs: Long = 0L,
    @SerialName("last_access_ts") val lastAccessTs: Long = 0L,
    @SerialName("quarantined_by") val quarantinedBy: String? = null,
    @SerialName("safe_from_quarantine") val safeFromQuarantine: Boolean = false,
)

@Serializable
data class MediaInfoResponse(
    @SerialName("media_id") val mediaId: String,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_length") val mediaLength: Long = 0L,
    @SerialName("upload_name") val uploadName: String? = null,
    @SerialName("created_ts") val createdTs: Long = 0L,
    @SerialName("last_access_ts") val lastAccessTs: Long = 0L,
    @SerialName("quarantined_by") val quarantinedBy: String? = null,
    @SerialName("safe_from_quarantine") val safeFromQuarantine: Boolean = false,
)

@Serializable
data class QuarantineResponse(
    @SerialName("num_quarantined") val numQuarantined: Int = 0,
)

@Serializable
data class DeleteMediaResponse(
    @SerialName("deleted_media") val deletedMedia: List<String> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class PurgeMediaCacheResponse(
    val deleted: Int = 0,
)
```

**Step 2: Create MediaAdminApi.kt**

```kotlin
package com.matrix.synapse.feature.media.data

import retrofit2.http.*

interface MediaAdminApi {

    @GET("/_synapse/admin/v1/room/{roomId}/media")
    suspend fun listRoomMedia(@Path("roomId") roomId: String): RoomMediaResponse

    @GET("/_synapse/admin/v1/media/{serverName}/{mediaId}")
    suspend fun getMediaInfo(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    ): MediaInfoResponse

    @POST("/_synapse/admin/v1/media/quarantine/{serverName}/{mediaId}")
    suspend fun quarantineMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    )

    @POST("/_synapse/admin/v1/media/unquarantine/{serverName}/{mediaId}")
    suspend fun unquarantineMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    )

    @POST("/_synapse/admin/v1/media/protect/{mediaId}")
    suspend fun protectMedia(@Path("mediaId") mediaId: String)

    @POST("/_synapse/admin/v1/media/unprotect/{mediaId}")
    suspend fun unprotectMedia(@Path("mediaId") mediaId: String)

    @DELETE("/_synapse/admin/v1/media/{serverName}/{mediaId}")
    suspend fun deleteMedia(
        @Path("serverName", encoded = true) serverName: String,
        @Path("mediaId") mediaId: String,
    ): DeleteMediaResponse

    @POST("/_synapse/admin/v1/media/delete")
    suspend fun bulkDeleteMedia(
        @Query("before_ts") beforeTs: Long,
        @Query("size_gt") sizeGt: Long? = null,
        @Query("keep_profiles") keepProfiles: Boolean? = null,
    ): DeleteMediaResponse

    @POST("/_synapse/admin/v1/purge_media_cache")
    suspend fun purgeRemoteMediaCache(
        @Query("before_ts") beforeTs: Long,
    ): PurgeMediaCacheResponse
}
```

**Step 3: Create MediaRepository.kt**

```kotlin
package com.matrix.synapse.feature.media.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): MediaAdminApi = retrofitFactory.create(serverUrl)

    suspend fun listRoomMedia(serverUrl: String, roomId: String): RoomMediaResponse =
        api(serverUrl).listRoomMedia(roomId)

    suspend fun getMediaInfo(serverUrl: String, serverName: String, mediaId: String): MediaInfoResponse =
        api(serverUrl).getMediaInfo(serverName, mediaId)

    suspend fun quarantineMedia(serverUrl: String, serverName: String, mediaId: String) =
        api(serverUrl).quarantineMedia(serverName, mediaId)

    suspend fun unquarantineMedia(serverUrl: String, serverName: String, mediaId: String) =
        api(serverUrl).unquarantineMedia(serverName, mediaId)

    suspend fun protectMedia(serverUrl: String, mediaId: String) =
        api(serverUrl).protectMedia(mediaId)

    suspend fun unprotectMedia(serverUrl: String, mediaId: String) =
        api(serverUrl).unprotectMedia(mediaId)

    suspend fun deleteMedia(serverUrl: String, serverName: String, mediaId: String): DeleteMediaResponse =
        api(serverUrl).deleteMedia(serverName, mediaId)

    suspend fun bulkDeleteMedia(
        serverUrl: String,
        beforeTs: Long,
        sizeGt: Long? = null,
        keepProfiles: Boolean? = null,
    ): DeleteMediaResponse = api(serverUrl).bulkDeleteMedia(beforeTs, sizeGt, keepProfiles)

    suspend fun purgeRemoteMediaCache(serverUrl: String, beforeTs: Long): PurgeMediaCacheResponse =
        api(serverUrl).purgeRemoteMediaCache(beforeTs)
}
```

**Step 4: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:media:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/media/src/main/kotlin/com/matrix/synapse/feature/media/data/
git commit -m "feat: add media data layer — API, models, repository"
```

### Task 7: Create MediaListViewModel and MediaDetailViewModel

**Files:**
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaListViewModel.kt`
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailViewModel.kt`

**Step 1: Create MediaListViewModel.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.DeleteMediaResponse
import com.matrix.synapse.feature.media.data.MediaRepository
import com.matrix.synapse.feature.media.data.PurgeMediaCacheResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaListItem(
    val mediaId: String,
    val origin: String,
    val isLocal: Boolean,
)

data class MediaListState(
    val mediaItems: List<MediaListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val filterMode: String = "room",
    val filterValue: String = "",
)

@HiltViewModel
class MediaListViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaListState())
    val state: StateFlow<MediaListState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun init(serverUrl: String, serverId: String, filterUserId: String?, filterRoomId: String?) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        when {
            filterRoomId != null -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = filterRoomId)
                loadRoomMedia(filterRoomId)
            }
            filterUserId != null -> {
                _state.value = _state.value.copy(filterMode = "user", filterValue = filterUserId)
            }
            else -> {
                _state.value = _state.value.copy(filterMode = "room", filterValue = "")
            }
        }
    }

    fun loadRoomMedia(roomId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, filterMode = "room", filterValue = roomId)
        viewModelScope.launch {
            runCatching {
                val serverName = extractServerName(serverUrl)
                val response = mediaRepository.listRoomMedia(serverUrl, roomId)
                val items = response.local.map { MediaListItem(mediaId = it, origin = serverName, isLocal = true) } +
                    response.remote.map { MediaListItem(mediaId = it, origin = serverName, isLocal = false) }
                items
            }.onSuccess { items ->
                _state.value = _state.value.copy(mediaItems = items, isLoading = false)
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun bulkDeleteMedia(beforeTs: Long, sizeGt: Long?, keepProfiles: Boolean?) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                mediaRepository.bulkDeleteMedia(serverUrl, beforeTs, sizeGt, keepProfiles)
            }.onSuccess { response ->
                _state.value = _state.value.copy(actionMessage = "Deleted ${response.total} media items")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.BULK_DELETE_MEDIA,
                        details = mapOf("deleted" to response.total.toString()),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun purgeRemoteMediaCache(beforeTs: Long) {
        _state.value = _state.value.copy(error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                mediaRepository.purgeRemoteMediaCache(serverUrl, beforeTs)
            }.onSuccess { response ->
                _state.value = _state.value.copy(actionMessage = "Purged ${response.deleted} remote media items")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.PURGE_REMOTE_MEDIA_CACHE,
                        details = mapOf("deleted" to response.deleted.toString()),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
```

**Step 2: Create MediaDetailViewModel.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.MediaInfoResponse
import com.matrix.synapse.feature.media.data.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailState(
    val media: MediaInfoResponse? = null,
    val isLoading: Boolean = false,
    val isActioning: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
)

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(MediaDetailState())
    val state: StateFlow<MediaDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun loadMedia(serverUrl: String, serverId: String, serverName: String, mediaId: String) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        _state.value = MediaDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                mediaRepository.getMediaInfo(serverUrl, serverName, mediaId)
            }.onSuccess { info ->
                _state.value = MediaDetailState(media = info)
            }.onFailure { e ->
                _state.value = MediaDetailState(error = e.message)
            }
        }
    }

    fun quarantine(serverName: String, mediaId: String) {
        performAction(AuditAction.QUARANTINE_MEDIA, mediaId) {
            mediaRepository.quarantineMedia(serverUrl, serverName, mediaId)
            "Media quarantined"
        }
    }

    fun unquarantine(serverName: String, mediaId: String) {
        performAction(AuditAction.UNQUARANTINE_MEDIA, mediaId) {
            mediaRepository.unquarantineMedia(serverUrl, serverName, mediaId)
            "Media removed from quarantine"
        }
    }

    fun protect(mediaId: String) {
        performAction(AuditAction.PROTECT_MEDIA, mediaId) {
            mediaRepository.protectMedia(serverUrl, mediaId)
            "Media protected from quarantine"
        }
    }

    fun unprotect(mediaId: String) {
        performAction(AuditAction.UNPROTECT_MEDIA, mediaId) {
            mediaRepository.unprotectMedia(serverUrl, mediaId)
            "Media protection removed"
        }
    }

    fun delete(serverName: String, mediaId: String) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                mediaRepository.deleteMedia(serverUrl, serverName, mediaId)
            }.onSuccess {
                _state.value = _state.value.copy(isActioning = false, isDeleted = true, actionMessage = "Media deleted")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.DELETE_MEDIA,
                        details = mapOf("media_id" to mediaId),
                    )
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isActioning = false, error = e.message)
            }
        }
    }

    private fun performAction(action: AuditAction, mediaId: String, block: suspend () -> String) {
        _state.value = _state.value.copy(isActioning = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { msg ->
                    _state.value = _state.value.copy(isActioning = false, actionMessage = msg)
                    auditLogger.insert(AuditLogEntry(serverId = serverId, action = action, details = mapOf("media_id" to mediaId)))
                    reloadMedia(mediaId)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(isActioning = false, error = e.message)
                }
        }
    }

    private fun reloadMedia(mediaId: String) {
        val serverName = extractServerName(serverUrl)
        viewModelScope.launch {
            runCatching { mediaRepository.getMediaInfo(serverUrl, serverName, mediaId) }
                .onSuccess { info -> _state.value = _state.value.copy(media = info) }
        }
    }

    private fun extractServerName(serverUrl: String): String =
        serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
}
```

**Step 3: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:media:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/
git commit -m "feat: add MediaListViewModel and MediaDetailViewModel"
```

### Task 8: Write media ViewModel tests

**Files:**
- Create: `feature/media/src/test/kotlin/com/matrix/synapse/feature/media/ui/MediaListViewModelTest.kt`
- Create: `feature/media/src/test/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailViewModelTest.kt`

**Step 1: Create MediaListViewModelTest.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaListViewModelTest {

    private val mediaRepository = mockk<MediaRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = MediaListViewModel(mediaRepository, auditLogger)

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadRoomMedia populates media items`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } returns RoomMediaResponse(
            local = listOf("abc123", "def456"),
            remote = listOf("ghi789"),
        )

        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", filterUserId = null, filterRoomId = "!room:example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(3, state.mediaItems.size)
            assertTrue(state.mediaItems[0].isLocal)
            assertFalse(state.mediaItems[2].isLocal)
        }
    }

    @Test
    fun `bulkDeleteMedia reports count and logs audit`() = runTest {
        coEvery { mediaRepository.bulkDeleteMedia(any(), any(), any(), any()) } returns DeleteMediaResponse(
            deletedMedia = listOf("a", "b"), total = 2,
        )

        val vm = createVm()
        vm.init("https://example.com", "srv1", null, null)
        vm.state.test {
            vm.bulkDeleteMedia(beforeTs = 1000L, sizeGt = null, keepProfiles = null)
            val state = expectMostRecentItem()
            assertEquals("Deleted 2 media items", state.actionMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.BULK_DELETE_MEDIA }) }
        }
    }

    @Test
    fun `purgeRemoteMediaCache reports count and logs audit`() = runTest {
        coEvery { mediaRepository.purgeRemoteMediaCache(any(), any()) } returns PurgeMediaCacheResponse(deleted = 5)

        val vm = createVm()
        vm.init("https://example.com", "srv1", null, null)
        vm.state.test {
            vm.purgeRemoteMediaCache(beforeTs = 1000L)
            val state = expectMostRecentItem()
            assertEquals("Purged 5 remote media items", state.actionMessage)
            coVerify { auditLogger.insert(match { it.action == AuditAction.PURGE_REMOTE_MEDIA_CACHE }) }
        }
    }

    @Test
    fun `loadRoomMedia sets error on failure`() = runTest {
        coEvery { mediaRepository.listRoomMedia(any(), any()) } throws RuntimeException("network error")

        val vm = createVm()
        vm.state.test {
            vm.init("https://example.com", "srv1", null, "!room:x")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
            assertFalse(state.isLoading)
        }
    }
}
```

**Step 2: Create MediaDetailViewModelTest.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.media.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaDetailViewModelTest {

    private val mediaRepository = mockk<MediaRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = MediaDetailViewModel(mediaRepository, auditLogger)

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadMedia populates detail`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(
            mediaId = "abc123", mediaType = "image/png", mediaLength = 1024,
        )

        val vm = createVm()
        vm.state.test {
            vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
            val state = expectMostRecentItem()
            assertEquals("abc123", state.media?.mediaId)
            assertEquals("image/png", state.media?.mediaType)
        }
    }

    @Test
    fun `quarantine logs audit and shows message`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(mediaId = "abc123")
        coEvery { mediaRepository.quarantineMedia(any(), any(), any()) } returns Unit

        val vm = createVm()
        vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
        vm.state.test {
            vm.quarantine("example.com", "abc123")
            val state = expectMostRecentItem()
            coVerify { auditLogger.insert(match { it.action == AuditAction.QUARANTINE_MEDIA }) }
        }
    }

    @Test
    fun `delete sets isDeleted and logs audit`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } returns MediaInfoResponse(mediaId = "abc123")
        coEvery { mediaRepository.deleteMedia(any(), any(), any()) } returns DeleteMediaResponse(total = 1)

        val vm = createVm()
        vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
        vm.state.test {
            vm.delete("example.com", "abc123")
            val state = expectMostRecentItem()
            assertTrue(state.isDeleted)
            coVerify { auditLogger.insert(match { it.action == AuditAction.DELETE_MEDIA }) }
        }
    }

    @Test
    fun `loadMedia sets error on failure`() = runTest {
        coEvery { mediaRepository.getMediaInfo(any(), any(), any()) } throws RuntimeException("not found")

        val vm = createVm()
        vm.state.test {
            vm.loadMedia("https://example.com", "srv1", "example.com", "abc123")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
```

**Step 3: Run tests**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:media:testDebugUnitTest`
Expected: 8 tests PASSED

**Step 4: Commit**

```bash
git add feature/media/src/test/kotlin/
git commit -m "test: add media ViewModel unit tests"
```

### Task 9: Create media screens

**Files:**
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaListScreen.kt`
- Create: `feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailScreen.kt`

**Step 1: Create MediaListScreen.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    serverUrl: String,
    serverId: String,
    filterUserId: String? = null,
    filterRoomId: String? = null,
    onMediaClick: (serverName: String, mediaId: String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: MediaListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl) { viewModel.init(serverUrl, serverId, filterUserId, filterRoomId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var roomIdInput by remember { mutableStateOf(filterRoomId ?: "") }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showPurgeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = {
                    TextButton(onClick = { showBulkDeleteDialog = true }) { Text("Bulk Delete") }
                    TextButton(onClick = { showPurgeDialog = true }) { Text("Purge Cache") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filterRoomId == null && filterUserId == null) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = roomIdInput,
                        onValueChange = { roomIdInput = it },
                        label = { Text("Room ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("media_room_input"),
                    )
                    Button(
                        onClick = { viewModel.loadRoomMedia(roomIdInput) },
                        enabled = roomIdInput.isNotBlank(),
                    ) { Text("Load") }
                }
            }

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(modifier = Modifier.testTag("media_list_loading")) }

                state.error != null && state.mediaItems.isEmpty() -> Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp).testTag("media_list_error"),
                )

                else -> LazyColumn(modifier = Modifier.testTag("media_list")) {
                    items(state.mediaItems, key = { it.mediaId }) { item ->
                        ListItem(
                            headlineContent = { Text(item.mediaId, maxLines = 1) },
                            supportingContent = {
                                Text(if (item.isLocal) "Local" else "Remote")
                            },
                            modifier = Modifier
                                .clickable { onMediaClick(item.origin, item.mediaId) }
                                .testTag("media_row_${item.mediaId}"),
                        )
                    }
                    if (state.mediaItems.isEmpty()) {
                        item {
                            Text(
                                "No media found",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        BulkDeleteDialog(
            onDismiss = { showBulkDeleteDialog = false },
            onConfirm = { days, sizeGt, keepProfiles ->
                showBulkDeleteDialog = false
                val beforeTs = System.currentTimeMillis() - (days * 86_400_000L)
                viewModel.bulkDeleteMedia(beforeTs, sizeGt, keepProfiles)
            },
        )
    }

    if (showPurgeDialog) {
        PurgeRemoteCacheDialog(
            onDismiss = { showPurgeDialog = false },
            onConfirm = { days ->
                showPurgeDialog = false
                val beforeTs = System.currentTimeMillis() - (days * 86_400_000L)
                viewModel.purgeRemoteMediaCache(beforeTs)
            },
        )
    }
}

@Composable
private fun BulkDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: (days: Long, sizeGt: Long?, keepProfiles: Boolean) -> Unit,
) {
    var daysText by remember { mutableStateOf("30") }
    var sizeText by remember { mutableStateOf("") }
    var keepProfiles by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Delete Local Media") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete local media not accessed within the specified period.")
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days old") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Minimum size (bytes, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = keepProfiles, onCheckedChange = { keepProfiles = it })
                    Text("Keep profile media")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toLongOrNull() ?: return@Button
                    val size = sizeText.toLongOrNull()
                    onConfirm(days, size, keepProfiles)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PurgeRemoteCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: (days: Long) -> Unit,
) {
    var daysText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Purge Remote Media Cache") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Purge cached remote media not accessed within the specified period.")
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days old") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { daysText.toLongOrNull()?.let { onConfirm(it) } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Purge") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
```

**Step 2: Create MediaDetailScreen.kt**

```kotlin
package com.matrix.synapse.feature.media.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    serverUrl: String,
    serverId: String,
    serverName: String,
    mediaId: String,
    onBack: () -> Unit = {},
    viewModel: MediaDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(mediaId) { viewModel.loadMedia(serverUrl, serverId, serverName, mediaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Detail") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.media == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            state.media != null -> {
                val media = state.media!!
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Media Info", style = MaterialTheme.typography.titleMedium)
                            InfoRow("Media ID", media.mediaId)
                            InfoRow("Type", media.mediaType ?: "unknown")
                            InfoRow("Size", formatBytes(media.mediaLength))
                            InfoRow("Upload Name", media.uploadName ?: "\u2014")
                            InfoRow("Created", formatTimestamp(media.createdTs))
                            InfoRow("Last Accessed", formatTimestamp(media.lastAccessTs))
                            InfoRow("Quarantined By", media.quarantinedBy ?: "No")
                            InfoRow("Protected", if (media.safeFromQuarantine) "Yes" else "No")
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Actions", style = MaterialTheme.typography.titleMedium)

                            if (media.quarantinedBy != null) {
                                Button(
                                    onClick = { viewModel.unquarantine(serverName, mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text("Remove from Quarantine") }
                            } else {
                                Button(
                                    onClick = { viewModel.quarantine(serverName, mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text("Quarantine") }
                            }

                            if (media.safeFromQuarantine) {
                                OutlinedButton(
                                    onClick = { viewModel.unprotect(mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text("Remove Protection") }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.protect(mediaId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !state.isActioning,
                                ) { Text("Protect from Quarantine") }
                            }

                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !state.isActioning,
                            ) { Text("Delete Media") }

                            if (state.isActioning) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("media_detail_progress"))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Media") },
            text = { Text("This will permanently delete this media item. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(serverName, mediaId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "\u2014"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
```

**Step 3: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:media:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaListScreen.kt \
       feature/media/src/main/kotlin/com/matrix/synapse/feature/media/ui/MediaDetailScreen.kt
git commit -m "feat: add MediaListScreen and MediaDetailScreen"
```

---

## Part 3: Federation Module

### Task 10: Scaffold `:feature:federation` module

**Files:**
- Create: `feature/federation/build.gradle.kts` (identical to media module pattern, namespace `com.matrix.synapse.feature.federation`)
- Create: `feature/federation/src/main/AndroidManifest.xml`
- Create: `feature/federation/consumer-rules.pro` (empty)
- Create: `feature/federation/proguard-rules.pro` (empty)
- Modify: `settings.gradle.kts` — add `include(":feature:federation")`
- Modify: `app/build.gradle.kts` — add `implementation(project(":feature:federation"))`

**Step 1: Create build.gradle.kts**

Same as media module but with `namespace = "com.matrix.synapse.feature.federation"`.

**Step 2: Create AndroidManifest.xml, proguard files** (same empty templates as media)

**Step 3: Add to settings.gradle.kts**

Add: `include(":feature:federation")`

**Step 4: Add to app/build.gradle.kts**

Add: `implementation(project(":feature:federation"))`

**Step 5: Sync check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:federation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add feature/federation/ settings.gradle.kts app/build.gradle.kts
git commit -m "feat: scaffold :feature:federation module"
```

### Task 11: Add federation audit action

**Files:**
- Modify: `core/database/src/main/kotlin/com/matrix/synapse/database/AuditAction.kt`

**Step 1: Add after `PURGE_REMOTE_MEDIA_CACHE,`**

```kotlin
RESET_FEDERATION_CONNECTION,
```

**Step 2: Compile and commit**

```bash
ANDROID_HOME=/home/user/android-sdk ./gradlew :core:database:compileDebugKotlin
git add core/database/src/main/kotlin/com/matrix/synapse/database/AuditAction.kt
git commit -m "feat: add federation audit action"
```

### Task 12: Create federation data layer (API, models, repository)

**Files:**
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/data/FederationModels.kt`
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/data/FederationAdminApi.kt`
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/data/FederationRepository.kt`

**Step 1: Create FederationModels.kt**

```kotlin
package com.matrix.synapse.feature.federation.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FederationDestination(
    val destination: String,
    @SerialName("retry_last_ts") val retryLastTs: Long = 0L,
    @SerialName("retry_interval") val retryInterval: Long = 0L,
    @SerialName("failure_ts") val failureTs: Long? = null,
    @SerialName("last_successful_stream_ordering") val lastSuccessfulStreamOrdering: Long? = null,
)

@Serializable
data class FederationDestinationsResponse(
    val destinations: List<FederationDestination> = emptyList(),
    val total: Int = 0,
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class DestinationRoom(
    @SerialName("room_id") val roomId: String,
    @SerialName("stream_ordering") val streamOrdering: Long = 0L,
)

@Serializable
data class DestinationRoomsResponse(
    val rooms: List<DestinationRoom> = emptyList(),
    val total: Int = 0,
    @SerialName("next_token") val nextToken: String? = null,
)
```

**Step 2: Create FederationAdminApi.kt**

```kotlin
package com.matrix.synapse.feature.federation.data

import retrofit2.http.*

interface FederationAdminApi {

    @GET("/_synapse/admin/v1/federation/destinations")
    suspend fun listDestinations(
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("order_by") orderBy: String? = null,
        @Query("dir") dir: String? = null,
    ): FederationDestinationsResponse

    @GET("/_synapse/admin/v1/federation/destinations/{destination}")
    suspend fun getDestination(
        @Path("destination") destination: String,
    ): FederationDestination

    @GET("/_synapse/admin/v1/federation/destinations/{destination}/rooms")
    suspend fun getDestinationRooms(
        @Path("destination") destination: String,
        @Query("from") from: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("dir") dir: String? = null,
    ): DestinationRoomsResponse

    @POST("/_synapse/admin/v1/federation/destinations/{destination}/reset_connection")
    suspend fun resetConnection(
        @Path("destination") destination: String,
    )
}
```

**Step 3: Create FederationRepository.kt**

```kotlin
package com.matrix.synapse.feature.federation.data

import com.matrix.synapse.network.RetrofitFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FederationRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
) {
    private fun api(serverUrl: String): FederationAdminApi = retrofitFactory.create(serverUrl)

    suspend fun listDestinations(
        serverUrl: String,
        from: String? = null,
        limit: Int = 100,
        orderBy: String? = null,
        dir: String? = null,
    ): FederationDestinationsResponse =
        api(serverUrl).listDestinations(from = from, limit = limit, orderBy = orderBy, dir = dir)

    suspend fun getDestination(serverUrl: String, destination: String): FederationDestination =
        api(serverUrl).getDestination(destination)

    suspend fun getDestinationRooms(
        serverUrl: String,
        destination: String,
        from: String? = null,
        limit: Int = 100,
    ): DestinationRoomsResponse =
        api(serverUrl).getDestinationRooms(destination, from = from, limit = limit)

    suspend fun resetConnection(serverUrl: String, destination: String) =
        api(serverUrl).resetConnection(destination)
}
```

**Step 4: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:federation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/data/
git commit -m "feat: add federation data layer — API, models, repository"
```

### Task 13: Create FederationListViewModel and FederationDetailViewModel

**Files:**
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationListViewModel.kt`
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationDetailViewModel.kt`

**Step 1: Create FederationListViewModel.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.feature.federation.data.FederationDestination
import com.matrix.synapse.feature.federation.data.FederationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FederationListState(
    val destinations: List<FederationDestination> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextToken: String? = null,
    val hasMore: Boolean = false,
    val totalDestinations: Int = 0,
    val sortBy: String = "destination",
    val sortDir: String = "f",
)

@HiltViewModel
class FederationListViewModel @Inject constructor(
    private val federationRepository: FederationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FederationListState())
    val state: StateFlow<FederationListState> = _state.asStateFlow()

    private var serverUrl: String = ""

    fun init(serverUrl: String) {
        this.serverUrl = serverUrl
        loadFirstPage()
    }

    fun setSort(orderBy: String, dir: String) {
        _state.value = _state.value.copy(
            sortBy = orderBy, sortDir = dir,
            destinations = emptyList(), nextToken = null, hasMore = false,
        )
        loadFirstPage()
    }

    fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore) return
        _state.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                federationRepository.listDestinations(
                    serverUrl,
                    from = current.nextToken,
                    orderBy = current.sortBy,
                    dir = current.sortDir,
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    destinations = _state.value.destinations + response.destinations,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalDestinations = response.total,
                    isLoadingMore = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoadingMore = false)
            }
        }
    }

    private fun loadFirstPage() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                federationRepository.listDestinations(
                    serverUrl,
                    orderBy = _state.value.sortBy,
                    dir = _state.value.sortDir,
                )
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    destinations = response.destinations,
                    nextToken = response.nextToken,
                    hasMore = response.nextToken != null,
                    totalDestinations = response.total,
                    isLoading = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }
}
```

**Step 2: Create FederationDetailViewModel.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogEntry
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.federation.data.DestinationRoom
import com.matrix.synapse.feature.federation.data.FederationDestination
import com.matrix.synapse.feature.federation.data.FederationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FederationDetailState(
    val destination: FederationDestination? = null,
    val rooms: List<DestinationRoom> = emptyList(),
    val isLoading: Boolean = false,
    val isResetting: Boolean = false,
    val isLoadingMoreRooms: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val totalRooms: Int = 0,
    val roomsNextToken: String? = null,
    val hasMoreRooms: Boolean = false,
)

@HiltViewModel
class FederationDetailViewModel @Inject constructor(
    private val federationRepository: FederationRepository,
    private val auditLogger: AuditLogger,
) : ViewModel() {

    private val _state = MutableStateFlow(FederationDetailState())
    val state: StateFlow<FederationDetailState> = _state.asStateFlow()

    private var serverUrl: String = ""
    private var serverId: String = ""

    fun loadDestination(serverUrl: String, serverId: String, destination: String) {
        this.serverUrl = serverUrl
        this.serverId = serverId
        _state.value = FederationDetailState(isLoading = true)
        viewModelScope.launch {
            runCatching {
                val dest = federationRepository.getDestination(serverUrl, destination)
                val rooms = federationRepository.getDestinationRooms(serverUrl, destination)
                dest to rooms
            }.onSuccess { (dest, rooms) ->
                _state.value = FederationDetailState(
                    destination = dest,
                    rooms = rooms.rooms,
                    totalRooms = rooms.total,
                    roomsNextToken = rooms.nextToken,
                    hasMoreRooms = rooms.nextToken != null,
                )
            }.onFailure { e ->
                _state.value = FederationDetailState(error = e.message)
            }
        }
    }

    fun resetConnection(destination: String) {
        _state.value = _state.value.copy(isResetting = true, error = null, actionMessage = null)
        viewModelScope.launch {
            runCatching {
                federationRepository.resetConnection(serverUrl, destination)
            }.onSuccess {
                _state.value = _state.value.copy(isResetting = false, actionMessage = "Connection reset initiated")
                auditLogger.insert(
                    AuditLogEntry(
                        serverId = serverId,
                        action = AuditAction.RESET_FEDERATION_CONNECTION,
                        details = mapOf("destination" to destination),
                    )
                )
                // Reload destination to get updated retry timing
                runCatching { federationRepository.getDestination(serverUrl, destination) }
                    .onSuccess { dest -> _state.value = _state.value.copy(destination = dest) }
            }.onFailure { e ->
                _state.value = _state.value.copy(isResetting = false, error = e.message)
            }
        }
    }

    fun loadMoreRooms(destination: String) {
        val current = _state.value
        if (current.isLoadingMoreRooms || !current.hasMoreRooms) return
        _state.value = current.copy(isLoadingMoreRooms = true)
        viewModelScope.launch {
            runCatching {
                federationRepository.getDestinationRooms(serverUrl, destination, from = current.roomsNextToken)
            }.onSuccess { response ->
                _state.value = _state.value.copy(
                    rooms = _state.value.rooms + response.rooms,
                    roomsNextToken = response.nextToken,
                    hasMoreRooms = response.nextToken != null,
                    totalRooms = response.total,
                    isLoadingMoreRooms = false,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message, isLoadingMoreRooms = false)
            }
        }
    }
}
```

**Step 3: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:federation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/
git commit -m "feat: add FederationListViewModel and FederationDetailViewModel"
```

### Task 14: Write federation ViewModel tests

**Files:**
- Create: `feature/federation/src/test/kotlin/com/matrix/synapse/feature/federation/ui/FederationListViewModelTest.kt`
- Create: `feature/federation/src/test/kotlin/com/matrix/synapse/feature/federation/ui/FederationDetailViewModelTest.kt`

**Step 1: Create FederationListViewModelTest.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import app.cash.turbine.test
import com.matrix.synapse.feature.federation.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FederationListViewModelTest {

    private val federationRepository = mockk<FederationRepository>()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `init loads first page of destinations`() = runTest {
        coEvery { federationRepository.listDestinations(any(), orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(
                destinations = listOf(
                    FederationDestination(destination = "matrix.org"),
                    FederationDestination(destination = "example.com", failureTs = 1000L),
                ),
                total = 2,
            )

        val vm = FederationListViewModel(federationRepository)
        vm.state.test {
            vm.init("https://example.com")
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.destinations.size)
            assertEquals("matrix.org", state.destinations[0].destination)
        }
    }

    @Test
    fun `loadNextPage appends destinations`() = runTest {
        coEvery { federationRepository.listDestinations(any(), from = isNull(), orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(
                destinations = listOf(FederationDestination(destination = "a.com")),
                total = 2,
                nextToken = "token1",
            )
        coEvery { federationRepository.listDestinations(any(), from = "token1", orderBy = any(), dir = any()) } returns
            FederationDestinationsResponse(
                destinations = listOf(FederationDestination(destination = "b.com")),
                total = 2,
            )

        val vm = FederationListViewModel(federationRepository)
        vm.init("https://example.com")
        vm.state.test {
            vm.loadNextPage()
            val state = expectMostRecentItem()
            assertEquals(2, state.destinations.size)
        }
    }

    @Test
    fun `error state set on failure`() = runTest {
        coEvery { federationRepository.listDestinations(any(), orderBy = any(), dir = any()) } throws
            RuntimeException("network error")

        val vm = FederationListViewModel(federationRepository)
        vm.state.test {
            vm.init("https://example.com")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
```

**Step 2: Create FederationDetailViewModelTest.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import app.cash.turbine.test
import com.matrix.synapse.database.AuditAction
import com.matrix.synapse.database.AuditLogger
import com.matrix.synapse.feature.federation.data.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FederationDetailViewModelTest {

    private val federationRepository = mockk<FederationRepository>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val dispatcher = UnconfinedTestDispatcher()

    private fun createVm() = FederationDetailViewModel(federationRepository, auditLogger)

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loadDestination populates detail and rooms`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } returns
            FederationDestination(destination = "matrix.org", failureTs = 1000L, retryInterval = 5000L)
        coEvery { federationRepository.getDestinationRooms(any(), any()) } returns
            DestinationRoomsResponse(
                rooms = listOf(DestinationRoom(roomId = "!room:matrix.org", streamOrdering = 42)),
                total = 1,
            )

        val vm = createVm()
        vm.state.test {
            vm.loadDestination("https://example.com", "srv1", "matrix.org")
            val state = expectMostRecentItem()
            assertEquals("matrix.org", state.destination?.destination)
            assertEquals(1, state.rooms.size)
            assertEquals(1000L, state.destination?.failureTs)
        }
    }

    @Test
    fun `resetConnection calls API and logs audit`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } returns
            FederationDestination(destination = "matrix.org")
        coEvery { federationRepository.getDestinationRooms(any(), any()) } returns
            DestinationRoomsResponse()
        coEvery { federationRepository.resetConnection(any(), any()) } returns Unit

        val vm = createVm()
        vm.loadDestination("https://example.com", "srv1", "matrix.org")
        vm.state.test {
            vm.resetConnection("matrix.org")
            val state = expectMostRecentItem()
            assertFalse(state.isResetting)
            coVerify { auditLogger.insert(match { it.action == AuditAction.RESET_FEDERATION_CONNECTION }) }
        }
    }

    @Test
    fun `loadDestination sets error on failure`() = runTest {
        coEvery { federationRepository.getDestination(any(), any()) } throws RuntimeException("not found")

        val vm = createVm()
        vm.state.test {
            vm.loadDestination("https://example.com", "srv1", "matrix.org")
            val state = expectMostRecentItem()
            assertNotNull(state.error)
        }
    }
}
```

**Step 3: Run tests**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:federation:testDebugUnitTest`
Expected: 6 tests PASSED

**Step 4: Commit**

```bash
git add feature/federation/src/test/kotlin/
git commit -m "test: add federation ViewModel unit tests"
```

### Task 15: Create federation screens

**Files:**
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationListScreen.kt`
- Create: `feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationDetailScreen.kt`

**Step 1: Create FederationListScreen.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.federation.data.FederationDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationListScreen(
    serverUrl: String,
    serverId: String,
    onDestinationClick: (destination: String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: FederationListViewModel = hiltViewModel(),
) {
    LaunchedEffect(serverUrl) { viewModel.init(serverUrl) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Federation (${state.totalDestinations})") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(modifier = Modifier.testTag("federation_list_loading")) }

            state.error != null -> Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp).testTag("federation_list_error"),
            )

            else -> {
                val listState = rememberLazyListState()

                LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
                    if (lastVisible >= state.destinations.size - 5 && state.hasMore && !state.isLoadingMore) {
                        viewModel.loadNextPage()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding).testTag("federation_list"),
                ) {
                    items(state.destinations, key = { it.destination }) { dest ->
                        DestinationRow(dest = dest, onClick = { onDestinationClick(dest.destination) })
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationRow(dest: FederationDestination, onClick: () -> Unit) {
    val healthColor = when {
        dest.failureTs == null -> Color(0xFF4CAF50)  // green — healthy
        dest.retryInterval > 0 -> Color(0xFFFFC107)  // yellow — retrying
        else -> Color(0xFFF44336)                      // red — failing
    }

    ListItem(
        headlineContent = { Text(dest.destination) },
        supportingContent = {
            if (dest.failureTs != null) {
                Text("Retry interval: ${formatInterval(dest.retryInterval)}")
            } else {
                Text("Healthy")
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier.size(12.dp),
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = healthColor)
                }
            }
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("federation_row_${dest.destination}"),
    )
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
```

**Step 2: Create FederationDetailScreen.kt**

```kotlin
package com.matrix.synapse.feature.federation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FederationDetailScreen(
    serverUrl: String,
    serverId: String,
    destination: String,
    onRoomClick: ((roomId: String) -> Unit)? = null,
    onBack: () -> Unit = {},
    viewModel: FederationDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(destination) { viewModel.loadDestination(serverUrl, serverId, destination) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.error != null && state.destination == null -> Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(padding).padding(16.dp),
            )

            state.destination != null -> {
                val dest = state.destination!!
                val healthColor = when {
                    dest.failureTs == null -> Color(0xFF4CAF50)
                    dest.retryInterval > 0 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Health header
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(modifier = Modifier.size(16.dp)) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(color = healthColor)
                                    }
                                }
                                Text(
                                    text = when {
                                        dest.failureTs == null -> "Healthy"
                                        dest.retryInterval > 0 -> "Retrying"
                                        else -> "Failing"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                    }

                    // Timing info
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Connection Info", style = MaterialTheme.typography.titleMedium)
                                InfoRow("First Failure", if (dest.failureTs != null) formatTimestamp(dest.failureTs) else "\u2014")
                                InfoRow("Last Retry", if (dest.retryLastTs > 0) formatTimestamp(dest.retryLastTs) else "\u2014")
                                InfoRow("Retry Interval", formatInterval(dest.retryInterval))
                            }
                        }
                    }

                    // Reset button
                    item {
                        Button(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isResetting,
                        ) {
                            if (state.isResetting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Reset Connection")
                            }
                        }
                    }

                    // Shared rooms
                    item {
                        Text("Shared Rooms (${state.totalRooms})", style = MaterialTheme.typography.titleMedium)
                    }

                    items(state.rooms, key = { it.roomId }) { room ->
                        ListItem(
                            headlineContent = { Text(room.roomId, maxLines = 1) },
                            modifier = if (onRoomClick != null) {
                                Modifier.clickable { onRoomClick(room.roomId) }
                            } else {
                                Modifier
                            }.testTag("federation_room_${room.roomId}"),
                        )
                    }

                    if (state.hasMoreRooms) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMoreRooms(destination) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Load More Rooms") }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Connection") },
            text = { Text("This will reset the retry timing for $destination and attempt to reconnect immediately.") },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    viewModel.resetConnection(destination)
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts == 0L) return "\u2014"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

private fun formatInterval(ms: Long): String = when {
    ms == 0L -> "none"
    ms < 60_000 -> "${ms / 1000}s"
    ms < 3_600_000 -> "${ms / 60_000}m"
    ms < 86_400_000 -> "${ms / 3_600_000}h"
    else -> "${ms / 86_400_000}d"
}
```

**Step 3: Compile check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew :feature:federation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationListScreen.kt \
       feature/federation/src/main/kotlin/com/matrix/synapse/feature/federation/ui/FederationDetailScreen.kt
git commit -m "feat: add FederationListScreen and FederationDetailScreen"
```

---

## Part 4: Navigation Wiring

### Task 16: Add routes and wire navigation

**Files:**
- Modify: `app/src/main/kotlin/com/matrix/synapse/manager/AppRoutes.kt`
- Modify: `app/src/main/kotlin/com/matrix/synapse/manager/AppNavHost.kt`
- Modify: `feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserListScreen.kt`
- Modify: `feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailScreen.kt`
- Modify: `feature/rooms/src/main/kotlin/com/matrix/synapse/feature/rooms/ui/RoomDetailScreen.kt`

**Step 1: Add new routes to AppRoutes.kt**

Add after `ServerDashboard` data class:

```kotlin
@Serializable
data class MediaList(
    val serverId: String,
    val serverUrl: String,
    val filterUserId: String? = null,
    val filterRoomId: String? = null,
)

@Serializable
data class MediaDetail(
    val serverId: String,
    val serverUrl: String,
    val serverName: String,
    val mediaId: String,
)

@Serializable
data class FederationList(val serverId: String, val serverUrl: String)

@Serializable
data class FederationDetail(val serverId: String, val serverUrl: String, val destination: String)
```

**Step 2: Add composable destinations to AppNavHost.kt**

Add imports:
```kotlin
import com.matrix.synapse.feature.media.ui.MediaListScreen
import com.matrix.synapse.feature.media.ui.MediaDetailScreen
import com.matrix.synapse.feature.federation.ui.FederationListScreen
import com.matrix.synapse.feature.federation.ui.FederationDetailScreen
```

Add after the `composable<ServerDashboard>` block:

```kotlin
composable<MediaList> { backStack ->
    val route = backStack.toRoute<MediaList>()
    MediaListScreen(
        serverUrl = route.serverUrl,
        serverId = route.serverId,
        filterUserId = route.filterUserId,
        filterRoomId = route.filterRoomId,
        onMediaClick = { serverName, mediaId ->
            navController.navigate(MediaDetail(route.serverId, route.serverUrl, serverName, mediaId))
        },
        onBack = { navController.popBackStack() },
    )
}

composable<MediaDetail> { backStack ->
    val route = backStack.toRoute<MediaDetail>()
    MediaDetailScreen(
        serverUrl = route.serverUrl,
        serverId = route.serverId,
        serverName = route.serverName,
        mediaId = route.mediaId,
        onBack = { navController.popBackStack() },
    )
}

composable<FederationList> { backStack ->
    val route = backStack.toRoute<FederationList>()
    FederationListScreen(
        serverUrl = route.serverUrl,
        serverId = route.serverId,
        onDestinationClick = { destination ->
            navController.navigate(FederationDetail(route.serverId, route.serverUrl, destination))
        },
        onBack = { navController.popBackStack() },
    )
}

composable<FederationDetail> { backStack ->
    val route = backStack.toRoute<FederationDetail>()
    FederationDetailScreen(
        serverUrl = route.serverUrl,
        serverId = route.serverId,
        destination = route.destination,
        onRoomClick = { roomId ->
            navController.navigate(RoomDetail(route.serverId, route.serverUrl, roomId))
        },
        onBack = { navController.popBackStack() },
    )
}
```

**Step 3: Add `onMedia` and `onFederation` callbacks to UserListScreen**

In `UserListScreen.kt`, add parameters after `onDashboard`:
```kotlin
onMedia: () -> Unit = {},
onFederation: () -> Unit = {},
```

In the `TopAppBar` actions, add before the Rooms button:
```kotlin
TextButton(onClick = onMedia) { Text("Media") }
TextButton(onClick = onFederation) { Text("Fed") }
```

**Step 4: Wire callbacks in AppNavHost.kt UserList composable**

Add to the `UserListScreen(...)` call inside `composable<UserList>`:
```kotlin
onMedia = {
    navController.navigate(MediaList(route.serverId, route.serverUrl))
},
onFederation = {
    navController.navigate(FederationList(route.serverId, route.serverUrl))
},
```

**Step 5: Add `onMedia` callback to UserDetailScreen**

In `UserDetailScreen.kt`, add parameter:
```kotlin
onMedia: () -> Unit = {},
```

Add a "Media" button after the "Whois / Sessions" button:
```kotlin
OutlinedButton(onClick = onMedia, modifier = Modifier.fillMaxWidth()) {
    Text("Media")
}
```

Wire in AppNavHost `composable<UserDetail>`:
```kotlin
onMedia = {
    navController.navigate(
        MediaList(route.serverId, route.serverUrl, filterUserId = route.userId)
    )
},
```

**Step 6: Add `onMedia` callback to RoomDetailScreen**

In `RoomDetailScreen.kt`, add parameter:
```kotlin
onMedia: () -> Unit = {},
```

Add a "Media" button in the actions card, after the "Make Me Room Admin" button:
```kotlin
Button(
    onClick = onMedia,
    modifier = Modifier.fillMaxWidth(),
) { Text("Room Media") }
```

Wire in AppNavHost `composable<RoomDetail>`:
```kotlin
onMedia = {
    navController.navigate(
        MediaList(route.serverId, route.serverUrl, filterRoomId = route.roomId)
    )
},
```

**Step 7: Full build check**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add app/src/main/kotlin/com/matrix/synapse/manager/AppRoutes.kt \
       app/src/main/kotlin/com/matrix/synapse/manager/AppNavHost.kt \
       feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserListScreen.kt \
       feature/users/src/main/kotlin/com/matrix/synapse/feature/users/ui/UserDetailScreen.kt \
       feature/rooms/src/main/kotlin/com/matrix/synapse/feature/rooms/ui/RoomDetailScreen.kt
git commit -m "feat: wire media and federation navigation — routes, nav graph, top bar buttons"
```

### Task 17: Run all tests and final verification

**Step 1: Run all unit tests**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew testDebugUnitTest`
Expected: All tests PASSED (existing + new)

**Step 2: Build debug APK**

Run: `ANDROID_HOME=/home/user/android-sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 3: Install and smoke test on device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Manual verification:
- [ ] Deactivate button visible on UserDetailScreen
- [ ] Deactivate dialog shows with delete-media checkbox
- [ ] Media button visible in top bar
- [ ] MediaListScreen loads with room ID input
- [ ] Media button on UserDetailScreen navigates with filter
- [ ] Media button on RoomDetailScreen navigates with filter
- [ ] Federation button visible in top bar
- [ ] FederationListScreen shows destinations with health indicators
- [ ] FederationDetailScreen shows timing info and rooms
- [ ] Reset Connection button works with confirmation

**Step 4: Commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: address smoke test issues"
```

---

## Summary

| Part | Tasks | New Files | Modified Files | Tests |
|------|-------|-----------|----------------|-------|
| 1. Deactivate | 1-3 | 1 test | 2 (ViewModel, Screen) | 2 |
| 2. Media | 4-9 | 8 source + 2 test + 4 scaffold | 2 (settings, app build) | 8 |
| 3. Federation | 10-15 | 8 source + 2 test + 4 scaffold | 2 (settings, app build) | 6 |
| 4. Navigation | 16-17 | 0 | 5 (routes, nav, screens) | 0 |
| **Total** | **17** | **29** | **11** | **16** |
