package com.don.homefitness.feature.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupServiceTest {
    private val service = BackupService()

    @Test fun `invalid json is rejected`() {
        assertThrows(BackupValidationException::class.java) { service.validateBackup(ByteArrayInputStream("{oops".toByteArray())) }
    }

    @Test fun `unknown version is rejected`() {
        val json = "{\"formatVersion\":2,\"exportedAt\":1,\"favorites\":[],\"plans\":[],\"sessions\":[]}"
        assertThrows(IllegalArgumentException::class.java) { service.validateBackup(ByteArrayInputStream(json.toByteArray())) }
    }

    @Test fun `round trip preserves document`() {
        val input = BackupDocument(1, 1, listOf("0001"), listOf(BackupPlan("p", "计划", listOf("0001"))))
        val output = service.encode(input)
        assertEquals(input, service.validateBackup(ByteArrayInputStream(output.toByteArray())).document)
    }
}
