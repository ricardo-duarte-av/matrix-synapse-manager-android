package com.matrix.synapse.manager

import org.junit.Assert.assertNotNull
import org.junit.Test

class BuildConfigSmokeTest {
    @Test
    fun app_build_config_is_loadable() {
        assertNotNull(BuildConfig.APPLICATION_ID)
    }
}
