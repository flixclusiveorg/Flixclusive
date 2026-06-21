# Rule 02: Architecture & Design Principles

> Read this when creating modules, features, or any structural code decisions.

---

## 1. MVVM + Clean Architecture

- **MVVM** is the UI architecture pattern. Every screen has a ViewModel that exposes state via `StateFlow`.
- **Clean Architecture** defines four layers with strict dependency direction:

```
┌─────────────┐
│   feature/   │  ← UI layer: Screens, ViewModels, composables
├─────────────┤
│   domain/    │  ← Business logic: Use cases
├─────────────┤
│    data/     │  ← Data access: Repositories, data sources, DTOs
├─────────────┤
│    core/     │  ← Shared: Utilities, database, network, navigation, presentation, strings
└─────────────┘
```

### Dependency Direction (Strictly Enforced)
- `feature` → `domain`, `data`, `core`
- `domain` → `data`, `core`
- `data` → `core`
- `core` → nothing above it

**NEVER** create an upward dependency (e.g., `data` importing from `feature`). This is a hard constraint — see `rules/01-anti-hallucination.md`, Rule 7.

---

## 2. SOLID Principles

### S — Single Responsibility
Each class and file has exactly **one reason to change**:
- **ViewModels** handle UI logic and state management only. No network calls, no DB queries directly.
- **Use cases** encapsulate a single piece of business logic. One use case = one operation.
- **Repositories** handle data access for a single data domain. No UI logic, no business rules.
- **Composables** render UI. No business logic inside composable functions.

### O — Open/Closed
- Prefer **interfaces** for repositories and use cases so that implementations can be swapped without changing consumers.
- Implementations are `internal` — consumers only see the interface.

### L — Liskov Substitution
- Repository and use case **interfaces** form the public API of a module.
- **Implementations** (suffixed `Impl`) are `internal` and bound to their interface via Hilt `@Binds`.
- Any implementation must be fully substitutable for its interface without breaking consumers.

### I — Interface Segregation
- Keep interfaces small and focused.
- Navigator interfaces should **compose** multiple small action interfaces rather than defining a single large interface:
  ```kotlin
  // ✅ Composed from small, single-action interfaces
  interface FeatureScreenNavigator :
      GoBackAction,
      StartHomeScreenAction,
      OpenSettingsAction

  // ❌ Monolithic interface with unrelated actions
  interface FeatureScreenNavigator {
      fun goBack()
      fun openHome()
      fun openSettings()
      fun openPlayer()
      fun toggleDarkMode()
  }
  ```

### D — Dependency Inversion
- ViewModels depend on **interfaces** (use cases, repositories), never on concrete implementations.
- Constructor injection via Hilt enforces this naturally.

---

## 3. Multi-Module Structure

The standard module layout:

```
core/                → Shared utilities, database, datastore, network, navigation, presentation, strings, drawables, testing
data/                → Repository interfaces + implementations, data sources, DTOs, mappers
domain/              → Use case interfaces + implementations, business logic
feature/             → UI feature modules organized by platform (mobile/, tv/)
build-logic/         → Convention plugins for consistent Gradle configuration across modules
```

### Rules
- **Multi-module projects:** Always use `build-logic/` with convention plugins. Never duplicate complex Gradle configuration across modules.
- **Single-module projects:** Use default project structure. No build-logic overhead needed.
- **Ask the developer** about the project's module structure if it's unclear. Different projects may have variations (e.g., no `domain/` layer, or `data/` and `domain/` merged).

---

## 4. Feature Development Approach

### Bottom-Up (Mandatory for New Features)
When building a feature from scratch, always work bottom-up through the layers:

1. **`data`** — Entities, DTOs, data sources, repository interface + implementation, DI module.
2. **`domain`** — Use case interface + implementation, DI module.
3. **`feature`** — ViewModel, UI state model, screen composables, navigator interface, previews, tests.

This ensures each layer is buildable and testable before the layer above it.

### TDD Priority
When creating features from scratch, **prioritize Test-Driven Development**:
- Write the test first (or alongside) for each unit: use case, repository, ViewModel.
- Tests validate the contract before the implementation is finalized.

### Avoid Premature Abstraction
- **Do NOT** create interfaces, abstract classes, or generic wrappers unless there is a clear, present need.
- Prefer concrete implementations that can be refactored into abstractions later.
- Abstraction should emerge from repeated patterns, not from speculation about future requirements.

### When Extending an Existing Feature
1. Read the existing feature's code thoroughly.
2. Follow the exact same patterns, naming, and structure.
3. If the feature doesn't have tests, ask the developer if they want tests added as part of the change.
