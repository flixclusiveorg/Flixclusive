# Rule 11: Naming Conventions

> Read this when naming files, packages, modules, classes, functions, or variables.

---

## 1. Package Names

- **Singular**, lowercase, dot-separated.
- Follow the module's established package root.

```
# ✅ CORRECT
com.example.feature.mobile.profile
com.example.domain.provider.usecase.get
com.example.data.download.repository

# ❌ WRONG — plural
com.example.feature.mobile.profiles
com.example.domain.providers
com.example.data.downloads
```

**Exception:** If a module already exists with a plural name, respect it for consistency. But for **new** modules and packages, always use singular.

---

## 2. Module Directories

- **Singular**, kebab-case.

```
# ✅ CORRECT
feature/mobile/user-edit
domain/provider
data/download
core/database

# ❌ WRONG — plural, or non-kebab
feature/mobile/users-edit
domain/providers
data/Downloads
core/data_base
```

---

## 3. File Naming

| Type | Pattern | Example |
|---|---|---|
| ViewModel | `<Feature>ViewModel.kt` | `HomeScreenViewModel.kt` |
| Screen composable | `<Feature>Screen.kt` | `HomeScreen.kt` |
| Navigator interface | `<Feature>ScreenNavigator.kt` | `HomeScreenNavigator.kt` |
| UI State (if separate file) | `<Feature>UiState.kt` | `HomeUiState.kt` |
| DI Module | `<Category>Module.kt` | `GetUseCasesModule.kt` |
| Use case interface | `<Verb><Noun>UseCase.kt` | `GetFilmMetadataUseCase.kt` |
| Use case implementation | `<Verb><Noun>UseCaseImpl.kt` | `GetFilmMetadataUseCaseImpl.kt` |
| Repository interface | `<Noun>Repository.kt` | `ProviderRepository.kt` |
| Repository implementation | `<Noun>RepositoryImpl.kt` | `ProviderRepositoryImpl.kt` |
| Room Entity | `<Noun>.kt` or `<Noun>Entity.kt` | `User.kt`, `WatchProgress.kt` |
| Room DAO | `<Noun>Dao.kt` | `UserDao.kt` |
| Sub-composable (extracted) | `<Component>.kt` | `EditButton.kt`, `GridMode.kt` |
| Utility | `<Purpose>Util.kt` | `UxUtil.kt`, `ModifierUtil.kt` |

---

## 4. Class & Interface Naming

### ViewModels
```kotlin
internal class HomeScreenViewModel  // ✅
internal class HomeVM              // ❌ — don't abbreviate
```

### Use Cases
```kotlin
interface GetFilmMetadataUseCase       // ✅ — Verb + Noun + UseCase
interface FilmMetadataGetter           // ❌ — non-standard naming
interface FetchFilmMetadata            // ❌ — missing "UseCase" suffix
```

### Repositories
```kotlin
interface ProviderRepository           // ✅ — Noun + Repository
interface ProviderRepo                 // ❌ — don't abbreviate
interface IProviderRepository          // ❌ — no "I" prefix for interfaces
```

### UI State
```kotlin
@Stable
internal data class HomeUiState(...)   // ✅ — Feature + UiState

internal data class HomeState(...)     // ❌ — ambiguous, could be domain state
internal data class HomeScreenData(...)// ❌ — non-standard suffix
```

### Navigator
```kotlin
interface HomeScreenNavigator : GoBackAction, OpenDetailsAction  // ✅
interface HomeNavigator                                          // ❌ — missing "Screen"
```

---

## 5. Function & Variable Naming

### Callbacks (Compose)
- Present tense verb, prefixed with `on`:
  ```kotlin
  onHoverProfile     // ✅
  onConsumeError     // ✅
  onNavigateBack     // ✅
  onProfileHovered   // ❌ — past tense
  onErrorConsumed    // ❌ — past tense
  ```

### ViewModel Functions
- Action-oriented, describing what the function does:
  ```kotlin
  fun onUseProfile(user: User)    // ✅ — describes user action
  fun onHoverProfile(user: User)  // ✅
  fun onConsumeErrors()           // ✅
  fun loadProviders()             // ✅ — internal operation
  fun handleClick()               // ❌ — vague
  fun doStuff()                   // ❌ — meaningless
  ```

### State Flow Properties
```kotlin
private val _uiState = MutableStateFlow(...)  // ✅ — underscore prefix for mutable
val uiState = _uiState.asStateFlow()          // ✅ — public immutable

private val _uiState = MutableStateFlow(...)
val uiState = _uiState                        // ❌ — exposes mutable
```

---

## 6. Consistency Rule

**When in doubt, read existing code.** The project's existing naming conventions take precedence over these rules. If the project uses a different pattern consistently (e.g., `*State` instead of `*UiState`), follow the project's pattern.
