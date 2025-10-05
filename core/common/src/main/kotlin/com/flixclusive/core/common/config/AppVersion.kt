package com.flixclusive.core.common.config

/**
 * Represents the build type of the application.
 *
 * - DEBUG: Development builds with debug features.
 * - PREVIEW: Pre-release builds for testing new features.
 * - RELEASE: Official production builds.
 * */
class AppVersion private constructor(
    private val buildType: BuildType,
    private val version: Comparable<*>,
) : Comparable<AppVersion> {
    override fun compareTo(other: AppVersion): Int {
        return if (version is Int && other.version is Int) {
            version.compareTo(other.version)
        } else if (version is SemVer && other.version is SemVer) {
            version.compareTo(other.version)
        } else {
            throw IllegalArgumentException(
                "Cannot compare different versions from different build types: $version and ${other.version}",
            )
        }
    }

    /**
     * Returns a formatted representation of the [AppVersion].
     *
     * - For PREVIEW builds, it prefixes the version with 'p' (e.g., "p5").
     * - For DEBUG builds, it prefixes the version with 'd' (e.g., "d5").
     * - For RELEASE builds, it returns the version as is (e.g "1.0.0").
     *
     * @return A string representation of the version.
     * */
    override fun toString(): String {
        return when (buildType) {
            BuildType.PREVIEW -> "p$version"
            BuildType.DEBUG -> "d$version"
            BuildType.STABLE -> version.toString()
        }
    }

    companion object {
        /**
         * Creates an [AppVersion] instance based on the build type and version string.
         *
         * - For PREVIEW and DEBUG builds, the version is derived from the commit count (assumed to be the first character of the build type determinant).
         * - For RELEASE builds, the version is parsed as a Semantic Version (SemVer).
         *
         * @param buildType The type of build (DEBUG, PREVIEW, RELEASE).
         * @param version The version string (commit count for DEBUG/PREVIEW, SemVer
         *
         * @return An [AppVersion] instance.
         * @throws IllegalArgumentException if the version format is invalid for RELEASE builds.
         * */
        fun from(
            buildType: BuildType,
            version: String,
        ): AppVersion {
            return when (buildType) {
                BuildType.PREVIEW, BuildType.DEBUG -> {
                    val commitCount = version.substring(1)

                    AppVersion(
                        buildType = buildType,
                        version = commitCount.toInt(),
                    )
                }

                BuildType.STABLE -> AppVersion(
                    buildType = buildType,
                    version = SemVer.from(version),
                )
            }
        }
    }
}

/**
 * A data class representing a Semantic Version (SemVer).
 *
 * @property major The major version number.
 * @property minor The minor version number.
 * @property patch The patch version number.
 * */
private data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)

        return 0
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    companion object {
        // Regex that captures major, minor, patch, and an optional pre-release tag.
        private val SEMVER_REGEX = """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""".toRegex()

        /**
         * Creates a SemVer instance from a version string.
         * @throws IllegalArgumentException if the version format is invalid.
         */
        fun from(version: String): SemVer {
            val match = SEMVER_REGEX.matchEntire(version.trim())
                ?: throw IllegalArgumentException("Invalid SemVer format: \"$version\"")

            val major = match.groupValues[1].toInt()
            val minor = match.groupValues[2].toInt()
            val patch = match.groupValues[3].toInt()

            return SemVer(major, minor, patch)
        }
    }
}
