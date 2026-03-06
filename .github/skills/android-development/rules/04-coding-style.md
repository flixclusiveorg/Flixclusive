# Rule 04: Coding Style (Non-UI Kotlin)

> Read this when writing any Kotlin code outside of Compose UI — ViewModels, repositories, use cases, data classes, utilities, DI modules.

---

## 1. Import Style

### Header-Level Imports Only
**NEVER** use fully qualified identifiers inline in code logic. All imports go at the top of the file.

```kotlin
// ✅ CORRECT
import com.example.core.strings.R as LocaleR

fun example() {
    val text = context.getString(LocaleR.string.some_label)
}

// ❌ WRONG — fully qualified inline
fun example() {
    val text = context.getString(com.example.core.strings.R.string.some_label)
}
```

### Aliased R Imports
When accessing Android resources (`R`) from other modules, use aliased imports to avoid ambiguity:

```kotlin
import com.example.core.drawables.R as DrawableR
import com.example.core.strings.R as StringR
```

The alias convention should match what the project already uses. **Read existing files** to discover the project's alias names before inventing your own.

---

## 2. Visibility Modifiers

### Default to the Narrowest Visibility
- **ViewModels:** `internal` — only the feature module's composables need access.
- **UI state classes:** `internal` — scoped to the feature module.
- **Repository/UseCase implementations:** `internal` — only exposed via their interface through DI.
- **DI modules:** `internal` — Hilt discovers them automatically.
- **Composable content functions:** `private` — only the screen's bridge function calls them.
- **`@Destination`-annotated composables:** `public` — required by the navigation framework.

### Why This Matters
Narrow visibility prevents accidental coupling between modules and makes refactoring safer. If an `internal` class is referenced from another module, the compiler catches it.

---

## 3. Repository Pattern

### Structure
```
data/<module>/
├── repository/
│   ├── SomeRepository.kt          ← Interface (public)
│   └── impl/
│       └── SomeRepositoryImpl.kt   ← Implementation (internal)
└── di/
    └── SomeModule.kt               ← Hilt DI module (internal)
```

### Interface
```kotlin
// Public, in data/<module>/repository/
interface SomeRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun getItem(id: String): Item?
    suspend fun save(item: Item)
}
```

### Implementation
```kotlin
// Internal, in data/<module>/repository/impl/
@Singleton
internal class SomeRepositoryImpl @Inject constructor(
    private val dao: SomeDao,
    private val appDispatchers: AppDispatchers,
) : SomeRepository {
    override fun observeItems(): Flow<List<Item>> = dao.observeAll()
    // ...
}
```

### DI Module
```kotlin
// Internal, in data/<module>/di/
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SomeModule {
    @Binds
    @Singleton
    abstract fun bindSomeRepository(impl: SomeRepositoryImpl): SomeRepository
}
```

---

## 4. Use Case Pattern

### Structure
```
domain/<module>/
├── usecase/
│   ├── <category>/
│   │   ├── DoSomethingUseCase.kt       ← Interface (public)
│   │   └── impl/
│   │       └── DoSomethingUseCaseImpl.kt ← Implementation (internal)
└── di/
    └── UseCasesModule.kt                 ← Hilt DI module (internal)
```

### Interface
```kotlin
// Public, with KDoc explaining the use case
/**
 * Use case for doing something specific.
 */
interface DoSomethingUseCase {
    suspend operator fun invoke(param: Param): Result
}
```

### Implementation
```kotlin
// Internal
internal class DoSomethingUseCaseImpl @Inject constructor(
    private val repository: SomeRepository,
    private val appDispatchers: AppDispatchers,
) : DoSomethingUseCase {
    override suspend fun invoke(param: Param): Result {
        return withContext(appDispatchers.io) {
            // business logic
        }
    }
}
```

### Key Rules
- Use `operator fun invoke(...)` so use cases can be called like functions: `doSomething(param)`.
- Use cases should contain **business logic only** — no UI concerns, no Android framework dependencies (except dispatchers).
- One use case = one operation. Do not bundle multiple unrelated operations.

---

## 5. ViewModel Pattern

### Structure
```kotlin
@HiltViewModel
internal class FeatureViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    private val someRepository: SomeRepository,
    appDispatchers: AppDispatchers,
) : ViewModel() {
    // Private mutable state
    private val _uiState = MutableStateFlow(FeatureUiState())
    // Public immutable state
    val uiState = _uiState.asStateFlow()

    // Derived state from other flows
    val items = someRepository.observeItems()
        .mapLatest { /* transform */ }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
}
```

### Key Rules
- **`@HiltViewModel` + `internal`** — always.
- **`StateFlow` over `mutableStateOf`** — prioritize `MutableStateFlow` + `.asStateFlow()` for state in ViewModels. This keeps the ViewModel pure Kotlin (no Compose dependency) and is more testable.
- **`SharingStarted`** — use `.WhileSubscribed(5000)` for flows that should stop when the UI is backgrounded, or `.Eagerly` for flows that must stay active.
- **Concurrent operation guards** — for operations triggered by user actions that should not overlap:
  ```kotlin
  private var loginJob: Job? = null

  fun onLogin(user: User) {
      if (loginJob?.isActive == true) return
      loginJob = viewModelScope.launch { /* ... */ }
  }
  ```
- **UI state class** — define at the bottom of the ViewModel file (or in a separate `<Feature>UiState.kt` if complex). Always annotate with `@Stable` or `@Immutable` (see `rules/05-compose-ui.md`).

---

## 6. Serialization

### Kotlin Serialization (`kotlinx.serialization`)
- Use for **DataStore**, internal data models, and anywhere Kotlin-native serialization is needed.
- Annotate data classes with `@Serializable`.
- The `kotlin-serialization` plugin is typically applied via a convention plugin or directly.

### Gson
- Use **only** for Retrofit network responses if the project uses `retrofit-gson` converter.
- Do NOT mix Gson annotations (`@SerializedName`) with `@Serializable` on the same class.

### Rule
- **Check which serialization framework the project uses** before generating serializable classes. Read existing DTOs and data models to confirm.

---

## 7. Error Handling in Data/Domain Layers

For operations that can fail (network, database, I/O), use a typed result wrapper to represent success/failure outcomes.

### Options (Ask the Developer)
```kotlin
// Option A: Kotlin's built-in Result<T>
suspend fun fetchData(): Result<Data>

// Option B: Custom sealed outcome (better for domain-specific semantics)
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: UiText) : Outcome<Nothing>
}
```

### Rules
- **Ask the developer** which pattern they prefer before generating code.
- **If the project already has a result wrapper** (e.g., `Resource`, `Result`, `Outcome`, `Either`), use it. Search the codebase first.
- **User-facing error messages** should use a string resource wrapper (e.g., `UiText`) — never raw strings for errors shown to users.
- **Avoid throwing exceptions** as control flow in data/domain layers. Wrap errors in the result type instead.
- **`try-catch` vs `runCatching`** — use `try-catch` only when you need to **do something specific** with the error (log it, transform it, recover from a specific exception type). If you just need to convert a throwing call into a `Result`, use `runCatching`:
  ```kotlin
  // ✅ Use runCatching — we only care about success/failure
  val result = runCatching { api.fetchData() }

  // ✅ Use try-catch — we inspect or act on the error
  try {
      api.fetchData()
  } catch (e: HttpException) {
      logger.error("HTTP ${e.code()}", e)
      emit(Outcome.Failure(UiText.from(e)))
  }

  // ❌ WRONG — try-catch that just wraps into Result without using the error
  try {
      val data = api.fetchData()
      Result.success(data)
  } catch (e: Exception) {
      Result.failure(e)
  }
  ```
