# Rule 09: Testing Practices

> Read this when writing or editing any test code — unit tests, ViewModel tests, integration tests.

---

## 1. Testing Stack

| Concern | Library | Rule |
|---|---|---|
| **Assertion** | `strikt` | Always. Never JUnit assertions (`assertEquals`), never Google Truth. |
| **Mocking** | `mockk` | Always. Never Mockito. |
| **Flow testing** | `turbine` | Always for Flow/StateFlow testing. |
| **Coroutines** | `kotlinx-coroutines-test` | `runTest`, `StandardTestDispatcher`, `advanceUntilIdle`. |
| **Network mocking** | `okhttp-mockwebserver` | For testing HTTP clients and Retrofit services. |

### Verify Availability
Before writing tests, check the project's version catalog and testing convention plugin to confirm these libraries are available. If any are missing, follow `rules/03-gradle.md` to add them.

---

## 2. ViewModel Test Pattern

### Full Template
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    // System under test
    private lateinit var viewModel: FeatureViewModel

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    // Mock dependencies
    private lateinit var someUseCase: SomeUseCase
    private lateinit var someRepository: SomeRepository
    private lateinit var appDispatchers: AppDispatchers

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        someUseCase = mockk(relaxed = true)
        someRepository = mockk(relaxed = true)

        // Replace all dispatchers with test dispatcher
        appDispatchers = object : AppDispatchers {
            override val default: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val main: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
            override val ioScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val defaultScope: CoroutineScope = CoroutineScope(testDispatcher)
            override val mainScope: CoroutineScope = CoroutineScope(testDispatcher)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Lazy creation — allows per-test mock setup before ViewModel init
    private fun createViewModel() {
        viewModel = FeatureViewModel(
            someUseCase = someUseCase,
            someRepository = someRepository,
            appDispatchers = appDispatchers,
        )
    }

    @Test
    fun `initial state should be correct`() = runTest(testDispatcher) {
        createViewModel()
        advanceUntilIdle()

        expectThat(viewModel.uiState.value) {
            get { isLoading }.isFalse()
            get { error }.isNull()
        }
    }
}
```

### Key Patterns

#### Lazy ViewModel Creation
Always create the ViewModel inside a `createViewModel()` helper, NOT in `@Before`:
```kotlin
private fun createViewModel() {
    viewModel = FeatureViewModel(/* deps */)
}
```
This allows each test to configure mocks differently before ViewModel initialization (which may trigger `init {}` blocks).

#### Test Dispatcher Setup
```kotlin
@Before
fun setup() {
    Dispatchers.setMain(testDispatcher)  // Required for viewModelScope
}

@After
fun tearDown() {
    Dispatchers.resetMain()  // Required cleanup
}
```

#### Custom AppDispatchers
If the project uses `AppDispatchers`, override all dispatchers with `testDispatcher` (see template above). This ensures deterministic execution.

---

## 3. Assertion Style (Strikt)

### Basic Assertions
```kotlin
expectThat(value).isEqualTo(expected)
expectThat(list).hasSize(3)
expectThat(flag).isTrue()
expectThat(nullableValue).isNull()
```

### Chained Property Assertions (Preferred for Data Classes)
```kotlin
expectThat(viewModel.uiState.value) {
    get { isLoading }.isFalse()
    get { items }.hasSize(2)
    get { error }.isNull()
    get { selectedItem }.isEqualTo(expectedItem)
}
```

### Collection Assertions
```kotlin
expectThat(profiles).hasSize(2)
expectThat(profiles[0]).isEqualTo(testUser1)
expectThat(profiles.none { it.id == excludedId }).isTrue()
```

### Anti-Patterns
```kotlin
// ❌ NEVER use JUnit assertions
assertEquals(expected, actual)
assertTrue(flag)
assertNull(value)

// ❌ NEVER use Google Truth
assertThat(value).isEqualTo(expected)
```

---

## 4. Mocking Style (MockK)

### Creating Mocks
```kotlin
// Relaxed mock — returns default values for all functions
private val repository = mockk<SomeRepository>(relaxed = true)

// Strict mock — throws on unconfigured calls (use when you need explicit setup)
private val useCase = mockk<SomeUseCase>()
```

### Configuring Mocks
```kotlin
// Suspend function
coEvery { repository.fetchItem(any()) } returns item

// Regular function
every { repository.observeItems() } returns flowOf(listOf(item1, item2))

// Void function
every { repository.clearAll() } just runs
```

### Verification
```kotlin
// Verify call happened
coVerify(exactly = 1) { repository.save(any()) }

// Verify call did NOT happen
coVerify(exactly = 0) { repository.delete(any()) }

// Verify call with specific argument
coVerify { repository.save(expectedItem) }
```

### Mocking Static/Extension Functions
```kotlin
// In @Before
mockkStatic("com.example.SomeExtensionKt")

// Configure
every { someObj.extensionFunction() } returns value
```

---

## 5. Flow Testing (Turbine)

### Single Flow Testing
```kotlin
@Test
fun `profiles should emit filtered list`() = runTest(testDispatcher) {
    every { repository.observeUsers() } returns flowOf(listOf(user1, user2, user3))

    createViewModel()
    advanceUntilIdle()

    viewModel.profiles.test {
        val profiles = awaitItem()
        expectThat(profiles).hasSize(2)
        expectThat(profiles[0]).isEqualTo(user1)
    }
}
```

### Two-Flow Testing with `turbineScope`
When you need to test two Flows simultaneously:
```kotlin
@Test
fun `both flows should update correctly`() = runTest(testDispatcher) {
    createViewModel()
    advanceUntilIdle()

    turbineScope {
        val uiStateTurbine = viewModel.uiState.testIn(this)
        val itemsTurbine = viewModel.items.testIn(this)

        // Trigger action
        viewModel.onRefresh()
        advanceUntilIdle()

        // Assert both flows
        expectThat(uiStateTurbine.awaitItem()) {
            get { isLoading }.isFalse()
        }
        expectThat(itemsTurbine.awaitItem()).hasSize(3)

        uiStateTurbine.cancelAndIgnoreRemainingEvents()
        itemsTurbine.cancelAndIgnoreRemainingEvents()
    }
}
```

### Key Rules
- **Always use `advanceUntilIdle()`** after triggering an action to let coroutines complete.
- **Always cancel turbines** when done: `.cancelAndIgnoreRemainingEvents()`.
- Use `awaitItem()` to get the next emission. Use `expectNoEvents()` to assert nothing was emitted.

---

## 6. Test Naming Convention

Use **backtick-quoted descriptive names** that describe the scenario and expected outcome:

```kotlin
@Test
fun `initial state should be correct`() { /* ... */ }

@Test
fun `onUseProfile should sign out current user and sign in new user`() { /* ... */ }

@Test
fun `loadProviders should handle failed provider initialization`() { /* ... */ }

@Test
fun `concurrent login operations should be prevented`() { /* ... */ }
```

### Pattern
`` `<action/scenario> should <expected outcome>` ``

---

## 7. Test Data

### Define Test Data as Properties
```kotlin
private val testUser = User(id = 1, name = "Test User", image = 0)
private val testItem = Item(id = "item-1", title = "Test Item")
```

### Use DummyData/Preview Utilities
If the project has a `DummyDataForPreview` or similar utility in a `testing` or `presentation/common` module, **use it** instead of recreating test data.

### Rules
- Search for existing test utilities and dummy data before creating your own.
- Keep test data as close to realistic values as possible — don't use placeholder text like "asdf" or "test123".
