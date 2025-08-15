package com.flixclusive.data.provider.repository.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.model.user.ProviderFromPreferences
import com.flixclusive.core.datastore.model.user.ProviderPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.impl.fake.TestProvider
import com.flixclusive.data.provider.repository.impl.fake.TestProvider.Companion.TEST_PROVIDER_ID
import com.flixclusive.data.provider.util.collections.CollectionsOperation
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import dalvik.system.PathClassLoader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class ProviderRepositoryImplTest {
    private lateinit var repository: ProviderRepositoryImpl
    private lateinit var dataStoreManager: DataStoreManager
    private val testDispatcher = StandardTestDispatcher()

    private val testProvider = TestProvider()
    private val mockClassLoader = mockk<PathClassLoader>()
    private val testMetadata = ProviderMetadata(
        id = TEST_PROVIDER_ID,
        name = "Test Provider",
        versionName = "1.0.0",
        authors = listOf(Author("Test Author")),
        repositoryUrl = "https://example.com/repo",
        description = "Test provider description",
        changelog = "Initial release",
        versionCode = 1,
        iconUrl = "https://example.com/icon.png",
        language = Language.Multiple,
        adult = false,
        status = Status.Working,
        buildUrl = "",
        providerType = ProviderType.All
    )
    private val testPreferenceItem = ProviderFromPreferences(
        id = TEST_PROVIDER_ID,
        filePath = "/path/to/test/provider.tmp",
        name = "Test Provider",
        isDisabled = false
    )

    @Before
    fun setup() {
        dataStoreManager = mockk(relaxed = true)
        repository = ProviderRepositoryImpl(dataStoreManager)
    }

    @Test
    fun shouldAddProviderWithAllComponents() = runTest(testDispatcher) {
        val transformSlot = slot<suspend (ProviderPreferences) -> ProviderPreferences>()
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = capture(transformSlot)
            )
        } returns Unit

        repository.add(
            provider = testProvider,
            classLoader = mockClassLoader,
            metadata = testMetadata,
            preferenceItem = testPreferenceItem
        )

        expectThat(repository.getProvider(testMetadata.id)).isEqualTo(testProvider)
        expectThat(repository.getProviderMetadata(testMetadata.id)).isEqualTo(testMetadata)
        expectThat(repository.getProviderFromPreferences(testMetadata.id)).isEqualTo(testPreferenceItem)

        coVerify {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = any()
            )
        }
    }

    @Test
    fun shouldNotAddDuplicatePreferenceItem() = runTest(testDispatcher) {
        val transformSlot = slot<suspend (ProviderPreferences) -> ProviderPreferences>()
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = capture(transformSlot)
            )
        } returns Unit

        repository.addToPreferences(testPreferenceItem)
        repository.addToPreferences(testPreferenceItem)

        // Should only call datastore once for the first addition
        coVerify(exactly = 1) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = any()
            )
        }
    }

    @Test
    fun shouldReturnNullForNonExistentProvider() = runTest(testDispatcher) {
        expectThat(repository.getProvider("non-existent")).isNull()
        expectThat(repository.getProviderMetadata("non-existent")).isNull()
        expectThat(repository.getProviderFromPreferences("non-existent")).isNull()
    }

    @Test
    fun shouldGetEnabledProvidersOnly() = runTest(testDispatcher) {
        val enabledMetadata = testMetadata.copy(id = "enabled-provider")
        val enabledPreference = testPreferenceItem.copy(id = "enabled-provider", isDisabled = false)

        val disabledMetadata = testMetadata.copy(id = "disabled-provider")
        val disabledPreference = testPreferenceItem.copy(id = "disabled-provider", isDisabled = true)

        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.add(testProvider, mockClassLoader, enabledMetadata, enabledPreference)
        repository.add(testProvider, mockClassLoader, disabledMetadata, disabledPreference)

        val enabledProviders = repository.getEnabledProviders()
        expectThat(enabledProviders).hasSize(1)
        expectThat(enabledProviders.first().id).isEqualTo("enabled-provider")
    }

    @Test
    fun shouldGetAllProviders() = runTest(testDispatcher) {
        val anotherMetadata = testMetadata.copy(id = "another-provider")
        val anotherPreference = testPreferenceItem.copy(id = "another-provider")

        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.add(testProvider, mockClassLoader, testMetadata, testPreferenceItem)
        repository.add(testProvider, mockClassLoader, anotherMetadata, anotherPreference)

        val allProviders = repository.getProviders()
        expectThat(allProviders).hasSize(2)
        expectThat(allProviders.map { it.id }).contains(TEST_PROVIDER_ID, "another-provider")
    }

    @Test
    fun shouldGetOrderedProviders() = runTest(testDispatcher) {
        val firstMetadata = testMetadata.copy(id = "first-provider")
        val firstPreference = testPreferenceItem.copy(id = "first-provider")

        val secondMetadata = testMetadata.copy(id = "second-provider")
        val secondPreference = testPreferenceItem.copy(id = "second-provider")

        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.add(testProvider, mockClassLoader, firstMetadata, firstPreference)
        repository.add(testProvider, mockClassLoader, secondMetadata, secondPreference)

        val orderedProviders = repository.getOrderedProviders()
        expectThat(orderedProviders).hasSize(2)
        expectThat(orderedProviders.first().id).isEqualTo("first-provider")
        expectThat(orderedProviders.last().id).isEqualTo("second-provider")
    }

    @Test
    fun shouldObserveOperations() = runTest(testDispatcher) {
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.observe().test {
            repository.addToPreferences(testPreferenceItem)

            val emission = awaitItem()
            expectThat(emission).isA<CollectionsOperation.List.Add<ProviderFromPreferences>>()
            expectThat((emission as CollectionsOperation.List.Add).item).isEqualTo(testPreferenceItem)
        }
    }

    @Test
    fun shouldMoveProvider() = runTest(testDispatcher) {
        val firstPreference = testPreferenceItem.copy(id = "first-provider")
        val secondPreference = testPreferenceItem.copy(id = "second-provider")

        val transformSlot = slot<suspend (ProviderPreferences) -> ProviderPreferences>()
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = capture(transformSlot)
            )
        } returns Unit

        repository.addToPreferences(firstPreference)
        repository.addToPreferences(secondPreference)

        repository.moveProvider(fromIndex = 0, toIndex = 1)

        coVerify(atLeast = 1) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = any()
            )
        }
    }

    @Test
    fun shouldRemoveProvider() = runTest(testDispatcher) {
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.add(testProvider, mockClassLoader, testMetadata, testPreferenceItem)
        repository.remove(testMetadata.id)

        expectThat(repository.getProvider(testMetadata.id)).isNull()
        expectThat(repository.getProviderMetadata(testMetadata.id)).isNull()
        expectThat(repository.getProviderFromPreferences(testMetadata.id)).isNull()
    }

    @Test
    fun shouldClearAll() = runTest(testDispatcher) {
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.add(testProvider, mockClassLoader, testMetadata, testPreferenceItem)
        repository.clearAll()

        expectThat(repository.getProviders()).hasSize(0)
        expectThat(repository.getProvider(testMetadata.id)).isNull()
    }

    @Test
    fun shouldRemoveFromPreferences() = runTest(testDispatcher) {
        val transformSlot = slot<suspend (ProviderPreferences) -> ProviderPreferences>()
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = capture(transformSlot)
            )
        } returns Unit

        repository.addToPreferences(testPreferenceItem)
        repository.removeFromPreferences(testMetadata.id)

        coVerify(exactly = 2) {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(
                key = UserPreferences.PROVIDER_PREFS_KEY,
                type = ProviderPreferences::class,
                transform = any()
            )
        }
    }

    @Test
    fun shouldToggleProvider() = runTest(testDispatcher) {
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.addToPreferences(testPreferenceItem)

        val initialState = repository.getProviderFromPreferences(testPreferenceItem.id)
        expectThat(initialState!!.isDisabled).isFalse()

        repository.toggleProvider(testPreferenceItem.id)

        val toggledState = repository.getProviderFromPreferences(testPreferenceItem.id)
        expectThat(toggledState!!.isDisabled).isTrue()
    }

    @Test
    fun shouldHandleProviderWithoutPreferenceItemInEnabledProviders() = runTest(testDispatcher) {
        repository.add(testProvider, mockClassLoader, testMetadata, testPreferenceItem)
        repository.removeFromPreferences(testMetadata.id)

        val enabledProviders = repository.getEnabledProviders()
        expectThat(enabledProviders).hasSize(0)
    }

    @Test
    fun shouldEmitRemoveOperationWhenProviderRemoved() = runTest(testDispatcher) {
        coEvery {
            dataStoreManager.updateUserPrefs<ProviderPreferences>(any(), any(), any())
        } returns Unit

        repository.addToPreferences(testPreferenceItem)

        repository.observe().test {
            repository.removeFromPreferences(testPreferenceItem.id)

            val emission = awaitItem()
            expectThat(emission).isA<CollectionsOperation.List.Remove<ProviderFromPreferences>>()
            expectThat((emission as CollectionsOperation.List.Remove).item.id).isEqualTo(testPreferenceItem.id)
        }
    }
}
