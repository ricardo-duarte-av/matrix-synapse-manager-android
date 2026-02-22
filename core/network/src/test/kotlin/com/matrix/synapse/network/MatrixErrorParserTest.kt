package com.matrix.synapse.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatrixErrorParserTest {

    private val parser = MatrixErrorParser()

    @Test
    fun parses_errcode_and_error_message() {
        val json = """{"errcode":"M_FORBIDDEN","error":"You are not a server admin"}"""
        val result = parser.parse(json)
        assertEquals("M_FORBIDDEN", result?.errcode)
        assertEquals("You are not a server admin", result?.error)
    }

    @Test
    fun returns_null_on_empty_body() {
        val result = parser.parse("")
        assertNull(result)
    }

    @Test
    fun returns_null_on_invalid_json() {
        val result = parser.parse("not-json")
        assertNull(result)
    }

    @Test
    fun returns_errcode_only_when_error_field_absent() {
        val json = """{"errcode":"M_UNKNOWN"}"""
        val result = parser.parse(json)
        assertEquals("M_UNKNOWN", result?.errcode)
        assertNull(result?.error)
    }

    @Test
    fun returns_null_for_non_matrix_json() {
        val json = """{"message":"ok"}"""
        val result = parser.parse(json)
        assertNull(result)
    }
}
