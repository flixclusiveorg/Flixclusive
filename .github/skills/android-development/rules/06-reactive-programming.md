# Rule 06: Reactive Programming (Flow & Coroutines)

> Read this when working with Flow, StateFlow, SharedFlow, coroutines, or any async logic.

---

## 1. Core Stack

- **Kotlin Coroutines** for all asynchronous operations.
- **Kotlin Flow** for all reactive streams.
- No RxJava. No LiveData in new code (use `StateFlow` instead).

---

## 2. Flow Types and When to Use Them

| Type | Use When |
|---|---|
| `StateFlow<T>` | Exposing UI state from ViewModel. Always has a current value. |
| `SharedFlow<T>` | Event-based streams (e.g., repository observation, one-shot events). |
| `Flow<T>` (cold) | Data that is produced on demand (e.g., database queries, paginated APIs). |

### ViewModel State Exposure
```kotlin
// Private mutable, public immutable
private val _uiState = MutableStateFlow(FeatureUiState())
val uiState = _uiState.asStateFlow()
```

### Derived State from Other Flows
```kotlin
val items = repository.observeItems()
    .mapLatest { /* transform */ }
    .distinctUntilChanged()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )
```

---

## 3. Common Flow Operators

Use these operators correctly and intentionally:

| Operator | Purpose | When to Use |
|---|---|---|
| `mapLatest` | Transform emissions, cancelling previous transform if a new emission arrives | Default choice for transforming flows in ViewModels |
| `flatMapLatest` | Switch to a new flow on each emission, cancelling the previous | When one flow's emission triggers a different flow (e.g., user change → new data) |
| `distinctUntilChanged` | Skip consecutive duplicate emissions | When upstream may emit the same value repeatedly |
| `filterNotNull` | Drop null emissions | When converting nullable to non-null flow |
| `onEach` | Side-effect on each emission without transforming | Logging, analytics, updating secondary state |
| `onCompletion` | Action when flow completes | Cleanup, final state updates |
| `stateIn` | Convert cold flow to hot StateFlow | Exposing flow as state in ViewModel |
| `shareIn` | Convert cold flow to hot SharedFlow | Sharing a flow across multiple collectors |

### Anti-Patterns
- **Do NOT** use `collect {}` inside `init {}` without a proper scope. Use `stateIn()` or `shareIn()` instead.
- **Do NOT** use `flowOf()` for state — it completes immediately. Use `MutableStateFlow` or `MutableSharedFlow`.
- **Do NOT** mix `combine` with `flatMapLatest` carelessly — understand the emission semantics.

---

## 4. Dispatchers

### Custom AppDispatchers Interface
Projects should define a custom `AppDispatchers` interface (injected via Hilt) for all coroutine context switching:

```kotlin
interface AppDispatchers {
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
    val ioScope: CoroutineScope
    val defaultScope: CoroutineScope
    val mainScope: CoroutineScope
}
```

### Why
- Enables full testability by replacing all dispatchers with `TestDispatcher` in tests.
- Avoids hardcoding `Dispatchers.IO` or `Dispatchers.Main` in production code.

### Rules
- **Search the project** for its `AppDispatchers` (or equivalent) before using `Dispatchers.*` directly.
- If the project uses `AppDispatchers`, always inject and use it — never use `Dispatchers.IO` directly.
- If the project does NOT have a custom dispatcher abstraction, use `Dispatchers.*` directly and note the testability trade-off.

---

## 5. Flow Collection in Compose

### Always Use `collectAsStateWithLifecycle()`
```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

### Why
- Lifecycle-aware: automatically stops collection when the composable is not visible.
- Prevents resource waste (e.g., network polling when the app is backgrounded).
- Requires `androidx.lifecycle:lifecycle-runtime-compose` — verify it's available.

### Anti-Patterns
- **Do NOT** use `.collectAsState()` — it's not lifecycle-aware.
- **Do NOT** collect flows inside `LaunchedEffect` for state that should survive recomposition — use `collectAsStateWithLifecycle()` instead.
- `LaunchedEffect` + `collect` is acceptable for one-shot side effects (e.g., navigation events, toasts).

---

## 6. Concurrency Patterns

### Job Guards
For user-triggered operations that should not run concurrently:

```kotlin
private var fetchJob: Job? = null

fun onRefresh() {
    if (fetchJob?.isActive == true) return
    fetchJob = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        // ... operation
        _uiState.update { it.copy(isLoading = false) }
    }
}
```

### Scoped Launches
- Use `viewModelScope.launch {}` for ViewModel operations.
- Use `appDispatchers.ioScope.launch {}` for operations that should outlive the ViewModel (rare — confirm with developer).
- Use `withContext(appDispatchers.io)` for suspending functions that need a specific dispatcher.

### State Updates
Use `.update {}` for atomic state mutations on `MutableStateFlow`:

```kotlin
_uiState.update { it.copy(isLoading = true, error = null) }
```

This is thread-safe and avoids race conditions from `.value = ...` assignments.
