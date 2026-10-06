package com.don.homefitness.data.body

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyModelValidatorTest {
    @Test
    fun missingModelIsRejected() {
        val manifest = manifest(entry(bytes = 3, sha256 = sha256("abc")))

        val result = BodyModelValidator().validate(manifest) { error("missing: $it") }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("does not exist") })
    }

    @Test
    fun sha256MismatchIsRejected() {
        val manifest = manifest(entry(bytes = 3, sha256 = "0".repeat(64)))

        val result = BodyModelValidator().validate(manifest) { "abc".toByteArray() }

        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("SHA-256 mismatch") })
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
        val male = entry(gender = "MALE", path = "body/male/body.glb", bytes = 3, sha256 = sha256("abc").uppercase())
        val female = entry(gender = "FEMALE", path = "body/female/body.glb", bytes = 4, sha256 = sha256("defg"))
        val manifest = "{\"entries\":[${male.json},${female.json}]}"

        val result = BodyModelValidator().validate(manifest) { path ->
            when (path) {
                "body/male/body.glb" -> "abc".toByteArray()
                "body/female/body.glb" -> "defg".toByteArray()
                else -> error("unexpected path: $path")
            }
        }

        assertTrue(result.valid)
        assertEquals(2, result.modelCount)
        assertEquals(7, result.totalBytes)
        assertTrue(result.errors.isEmpty())
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
    ) = TestEntry(
        "{\"gender\":\"$gender\",\"assetPath\":\"$path\",\"sha256\":\"$sha256\",\"bytes\":$bytes,\"licenseStatus\":\"$licenseStatus\",\"sourceUrlOrRepository\":\"$sourceUrlOrRepository\",\"sourceCommitOrVersion\":\"$sourceCommitOrVersion\",\"license\":\"$license\",\"attribution\":\"$attribution\",\"apkRedistributionAuthorization\":\"$apkRedistributionAuthorization\"}",
    )

    private data class TestEntry(val json: String)

    private companion object {
        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
