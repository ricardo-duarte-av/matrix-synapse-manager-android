package com.matrix.synapse.network

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveTokenHolder @Inject constructor() {
    private val _token = AtomicReference<String?>(null)
    fun get(): String? = _token.get()
    fun set(token: String?) = _token.set(token)
}
