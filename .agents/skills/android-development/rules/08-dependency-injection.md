# Rule 08: Dependency Injection (Hilt)

> Read this when creating DI modules, injecting dependencies, or making scoping decisions.

---

## 1. DI Framework

- **Hilt** is the standard DI framework for Android projects using this skill.
- If the project does NOT use Hilt, search for the actual DI framework (Koin, manual DI, Dagger standalone) and follow its patterns instead.

---

## 2. DI Module Patterns

### Data Layer — Repository Binding
```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindSomeRepository(impl: SomeRepositoryImpl): SomeRepository
}
```

### Domain Layer — Use Case Binding
```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal abstract class UseCasesModule {
    @Binds
    abstract fun bindDoSomethingUseCase(impl: DoSomethingUseCaseImpl): DoSomethingUseCase

    @Binds
    @Singleton
    abstract fun bindStatefulUseCase(impl: StatefulUseCaseImpl): StatefulUseCase
}
```

### Feature Layer — No DI Module Needed
ViewModels annotated with `@HiltViewModel` are automatically provided by Hilt. No manual DI module is required.

### Key Rules
- **`@Binds` over `@Provides`** when binding an implementation to an interface. `@Provides` is for third-party classes or complex construction only.
- **DI modules are `internal abstract class`es** — Hilt discovers them via annotation processing.
- **One DI module per concern/category** — don't create a god module. Split by purpose (e.g., `GetUseCasesModule`, `ManageUseCasesModule`, `RepositoryModule`).

---

## 3. Scope Rules

| Component | Scope | Rationale |
|---|---|---|
| Repositories | `@Singleton` | Shared state, caching, single source of truth |
| Stateful Use Cases | `@Singleton` | Holds state that outlives individual ViewModels |
| Stateless Use Cases | Unscoped (default) | Created per injection site, no shared state — cheaper |
| ViewModels | `@HiltViewModel` (nav-scoped) | Scoped to the navigation graph by Hilt |
| Data Sources / DAOs | `@Singleton` (via Room) | Database is a singleton |

### Rules
- **Do NOT over-scope.** If a use case is stateless, leave it unscoped. `@Singleton` creates a permanent memory allocation.
- **Do NOT scope ViewModels manually.** `@HiltViewModel` handles this.
- If unsure about scoping, prefer **unscoped** — it's the safer default.

---

## 4. Constructor Injection

### Preferred: Constructor Injection
```kotlin
// ✅ Constructor injection — testable, clear dependencies
@Singleton
internal class SomeRepositoryImpl @Inject constructor(
    private val dao: SomeDao,
    private val api: SomeApi,
    private val appDispatchers: AppDispatchers,
) : SomeRepository { /* ... */ }
```

### Avoid: Field Injection
```kotlin
// ❌ Field injection — harder to test, hidden dependencies
internal class SomeRepositoryImpl : SomeRepository {
    @Inject lateinit var dao: SomeDao
    @Inject lateinit var api: SomeApi
}
```

### Rules
- **Always use constructor injection** for classes you control.
- **Field injection** is only acceptable for Android framework classes where constructor injection is impossible (e.g., `Activity`, `Fragment`, `Service`).

---

## 5. Providing Third-Party Dependencies

When a class cannot be constructor-injected (e.g., OkHttpClient, Retrofit, Room database):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

### Rules
- Use `object` (not `abstract class`) for modules with `@Provides` functions.
- Use `abstract class` for modules with `@Binds` functions.
- **NEVER mix `@Provides` and `@Binds` in the same module class.** Split them if needed:
  - `abstract class` for `@Binds`
  - Companion `object` for `@Provides` within the same module (Hilt supports this)
