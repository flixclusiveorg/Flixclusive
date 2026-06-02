package com.flixclusive.data.provider.repository.impl

/**
 * Unit tests for [MediaLinksRepositoryImpl] are pending a rewrite.
 *
 * The repository was migrated from an in-memory design to a Room-backed
 * implementation. The old tests referenced types (MediaLinks, MediaLinksCacheKey,
 * repository.caches, repository.addStream) that no longer exist.
 *
 * New tests should use an in-memory Room database (via Room.inMemoryDatabaseBuilder)
 * and the updated MediaLinksRepository interface.
 */
class MediaLinksRepositoryImplTest
