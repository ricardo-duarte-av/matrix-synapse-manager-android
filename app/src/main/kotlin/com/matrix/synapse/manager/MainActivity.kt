package com.matrix.synapse.manager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.matrix.synapse.manager.ui.theme.MatrixSynapseManagerTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.matrix.synapse.feature.settings.security.AppLockManager
import com.matrix.synapse.manager.tabs.TabOrderRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var tabOrderRepository: TabOrderRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatrixSynapseManagerTheme {
                val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

                if (isLocked) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    AppNavHost(
                        modifier = Modifier.fillMaxSize(),
                        tabOrderRepository = tabOrderRepository,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appLockManager.lock()
        if (appLockManager.isLocked.value) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    appLockManager.unlock()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Synapse Manager")
            .setSubtitle("Authenticate to access admin controls")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }
}
