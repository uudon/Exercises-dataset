package com.don.homefitness.data.body

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyModelValidatorTest {
    @Test
    fun malformedManifestIsBlockedAndRuntimeInvalid() {
        val result = BodyModelValidator().validate("{not-json") { error("must not read assets") }

        assertFalse(result.valid)
        assertFalse(result.runtimeValid)
        assertFalse(result.structureValid)
        assertEquals("blocked", result.authorization.releaseGate)
    }

    @Test
    fun missingModelIsRejected() {
        val manifest = manifest(entry(bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { error("missing: $it") }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("does not exist") })
        assertFalse(result.runtimeValid)
    }

    @Test
    fun sha256MismatchIsRejected() {
        val manifest = manifest(entry(bytes = 3, sha256 = "0".repeat(64)))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("SHA-256 mismatch") })
        assertFalse(result.runtimeValid)
    }

    @Test
    fun remotePathIsRejectedWithoutCallingReader() {
        val manifest = manifest(entry(path = "https://example.test/body.glb", bytes = 3, sha256 = sha256("abc")))
        var called = false

        val result = BodyModelValidator().validate(manifest) {
            called = true
            "abc".toByteArray()
        }

        assertFalse(result.valid)
        assertFalse(called)
        assertTrue(result.errors.any { it.contains("relative android_asset path") })
    }

    @Test
    fun unconfirmedLicenseIsRejected() {
        val manifest = manifest(entry(licenseStatus = "unconfirmed", bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("licenseStatus must be confirmed") })
    }

    @Test
    fun missingSourceMetadataIsRejected() {
        val manifest = manifest(entry(sourceUrlOrRepository = "", bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("source URL or repository is missing or blocked") })
    }

    @Test
    fun blockedApkAuthorizationIsRejected() {
        val manifest = manifest(entry(apkRedistributionAuthorization = "blocked", bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("APK redistribution authorization must be confirmed") })
    }

    @Test
    fun blockedMetadataComparisonTrimsAndIgnoresCase() {
        val manifest = manifest(entry(sourceUrlOrRepository = "  BlOcKeD  ", bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("source URL or repository is missing or blocked") })
    }

    @Test
    fun malformedSha256IsRejected() {
        val result = BodyModelValidator().validate(manifest(entry(sha256 = "not-a-sha", bytes = 3))) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("has an invalid SHA-256") })
    }

    @Test
    fun negativeByteDeclarationIsRejected() {
        val result = BodyModelValidator().validate(manifest(entry(bytes = -1, sha256 = sha256("abc")))) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("negative byte count") })
        assertFalse(result.runtimeValid)
    }

    @Test
    fun byteCountMismatchDisablesRuntime() {
        val result = BodyModelValidator().validate(manifest(entry(bytes = 4, sha256 = sha256("abc")))) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertFalse(result.runtimeValid)
        assertTrue(result.errors.any { it.contains("byte count mismatch") })
    }

    @Test
    fun corruptGlbDisablesRuntime() {
        val valid = minimalGlb()
        val corrupt = valid.copyOf().also { it[0] = 'x'.code.toByte() }
        val result = BodyModelValidator().validate(manifest(entry(bytes = corrupt.size, sha256 = sha256(corrupt)))) { corrupt }

        assertFalse(result.valid)
        assertFalse(result.runtimeValid)
        assertEquals("blocked", result.authorization.releaseGate)
        assertTrue(result.errors.any { it.contains("GLB structure is invalid") })
    }

    @Test
    fun duplicateGenderIsRejected() {
        val first = entry(gender = "MALE", bytes = 3, sha256 = sha256("abc"))
        val second = entry(gender = "MALE", path = "body/male/other.glb", bytes = 3, sha256 = sha256("abc"))

        val result = BodyModelValidator().validate("{\"entries\":[${first.json},${second.json}]}") { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("MALE model is not unique") })
    }

    @Test
    fun twoValidModelsAreAccepted() {
        val model = minimalGlb()
        val male = entry(gender = "MALE", path = "body/male/body.glb", bytes = model.size, sha256 = sha256(model))
        val female = entry(gender = "FEMALE", path = "body/female/body.glb", bytes = model.size, sha256 = sha256(model))
        val manifest = "{\"entries\":[${male.json},${female.json}]}"

        val result = BodyModelValidator().validate(manifest) { path ->
            when (path) {
                "body/male/body.glb", "body/female/body.glb" -> model
                else -> error("unexpected path: $path")
            }
        }

        assertTrue(result.valid)
        assertEquals(2, result.modelCount)
        assertEquals(model.size.toLong() * 2, result.totalBytes)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun staleValidAndConfirmedAuthorizationSidecarFailsClosedAgainstBlockedManifest() {
        val expected = BodyModelCheckReport(
            valid = false,
            errors = listOf("licenseStatus is not confirmed"),
            modelCount = 0,
            totalBytes = 0,
            runtimeValid = true,
            structureValid = true,
            licenseStatus = "blocked",
            authorization = BodyModelAuthorization(
                licenseStatus = "blocked",
                apkRedistribution = "blocked",
                releaseGate = "blocked",
            ),
        )
        val reported = expected.copy(
            valid = true,
            licenseStatus = "confirmed",
            authorization = BodyModelAuthorization(
                licenseStatus = "confirmed",
                apkRedistribution = "confirmed",
                releaseGate = "authorized",
            ),
        )

        val errors = BodyModelValidator().validateReportIntegrity(expected, reported)

        assertTrue(errors.any { it.contains("valid") })
        assertTrue(errors.any { it.contains("licenseStatus") })
        assertTrue(errors.any { it.contains("authorization") })
        assertTrue(errors.none { it.contains("runtimeValid") || it.contains("structureValid") })
    }

    private fun manifest(entry: TestEntry): String = "{\"entries\":[${entry.json}]}"

    private fun entry(
        gender: String = "MALE",
        path: String = "body/male/body.glb",
        bytes: Int = 3,
        sha256: String = sha256("abc"),
        licenseStatus: String = "confirmed",
        sourceUrlOrRepository: String = "https://example.test/body",
        sourceCommitOrVersion: String = "v1.0.0",
        license: String = "CC-BY-4.0",
        attribution: String = "Example Author",
        apkRedistributionAuthorization: String = "confirmed",
        meshes: Int = 1,
        materials: Int = 1,
        triangles: Int = 1,
        selectableMeshes: Int = 1,
        requiredRegions: String = "[\"abs\"]",
    ) = TestEntry(
        "{\"gender\":\"$gender\",\"assetPath\":\"$path\",\"sha256\":\"$sha256\",\"bytes\":$bytes,\"licenseStatus\":\"$licenseStatus\",\"sourceUrlOrRepository\":\"$sourceUrlOrRepository\",\"sourceCommitOrVersion\":\"$sourceCommitOrVersion\",\"license\":\"$license\",\"attribution\":\"$attribution\",\"apkRedistributionAuthorization\":\"$apkRedistributionAuthorization\",\"meshes\":$meshes,\"materials\":$materials,\"triangles\":$triangles,\"selectableMeshes\":$selectableMeshes,\"requiredRegions\":$requiredRegions}",
    )

    private data class TestEntry(val json: String)

    private fun minimalGlb(): ByteArray {
        val positions = java.nio.ByteBuffer.allocate(36).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putFloat(0f).putFloat(0f).putFloat(0f)
            .putFloat(1f).putFloat(0f).putFloat(0f)
            .putFloat(0f).putFloat(1f).putFloat(0f).array()
        val bin = positions + byteArrayOf(0, 1, 2)
        val json = "{\"asset\":{\"version\":\"2.0\"},\"buffers\":[{\"byteLength\":40}],\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":36},{\"buffer\":0,\"byteOffset\":36,\"byteLength\":3}],\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"},{\"bufferView\":1,\"componentType\":5121,\"count\":3,\"type\":\"SCALAR\"}],\"materials\":[{}],\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0},\"indices\":1,\"material\":0}]}],\"nodes\":[{\"name\":\"abs\",\"mesh\":0,\"extras\":{\"region_id\":\"abs\",\"selectable\":true}}]}".toByteArray()
        val paddedJson = json + ByteArray((4 - json.size % 4) % 4) { 0x20 }
        val paddedBin = bin + ByteArray((4 - bin.size % 4) % 4)
        val length = 12 + 8 + paddedJson.size + 8 + paddedBin.size
        return java.nio.ByteBuffer.allocate(length).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putInt(0x46546C67)
            .putInt(2)
            .putInt(length)
            .putInt(paddedJson.size)
            .putInt(0x4E4F534A)
            .put(paddedJson)
            .putInt(paddedBin.size)
            .putInt(0x004E4942)
            .put(paddedBin)
            .array()
    }

    private companion object {
        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

        fun sha256(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(value)
            .joinToString("") { "%02x".format(it) }
    }
}
