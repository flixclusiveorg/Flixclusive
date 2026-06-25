# Rule 07: Navigation

> Read this when adding screens, routes, or navigation logic.

---

## 1. Navigation Library

### Determine the Project's Navigation Library First
Before generating any navigation code, **search the project** to determine which navigation library is in use:

| Library | How to Detect |
|---|---|
| **Compose Destinations** (raamcosta) | Look for `@Destination` annotations, `destinations-core` in version catalog |
| **Official Jetpack Navigation Compose** | Look for `NavHost`, `composable()` route builders, `navigation-compose` in version catalog |
| **Voyager** | Look for `Screen` interface, `Navigator`, `voyager` in version catalog |
| **Decompose** | Look for `ComponentContext`, `decompose` in version catalog |

**Do NOT mix navigation libraries.** Use whatever the project already uses.

---

## 2. Navigator Interface Pattern

Each feature screen should define a `*ScreenNavigator` interface that composes small, single-action interfaces.

### Structure
```kotlin
// In the feature module
interface FeatureScreenNavigator :
    GoBackAction,
    OpenDetailsAction,
    OpenSettingsAction
```

### Small Action Interfaces (Defined in a Core/Navigation Module)
```kotlin
// In core/navigation or equivalent
interface GoBackAction {
    fun goBack()
}

interface OpenDetailsAction {
    fun openDetails(id: String)
}
```

### Rules
- **Search the project's navigation/core module** for existing action interfaces before creating new ones.
- Action interfaces should be **single-method** — one interface per navigation action.
- The actual implementation of the navigator is provided by the app module's navigation graph, not by the feature module.

---

## 3. Navigation Arguments

### Passing Data Between Screens
- Navigation arguments are passed via `@Destination` annotation parameters (Compose Destinations) or route arguments (Jetpack Nav).
- For return results, use `OpenResultRecipient` (Compose Destinations) or `SavedStateHandle` (Jetpack Nav).
- **NEVER pass complex objects as navigation arguments.** Pass IDs and let the destination screen fetch the data.

### Example (Compose Destinations)
```kotlin
@Destination<ExternalModuleGraph>
@Composable
fun FeatureScreen(
    navigator: FeatureScreenNavigator,
    itemId: String,  // Simple argument
    resultRecipient: OpenResultRecipient<Boolean>,  // Return result
) {
    resultRecipient.onNavResult { result ->
        if (result is NavResult.Value && result.value) {
            // Handle result
        }
    }
    // ...
}
```

---

## 4. Navigation Module Configuration

If the project uses Compose Destinations:
- A convention plugin (e.g., `flixclusive.destinations`) typically applies KSP and the destinations dependency.
- Each feature module that contains `@Destination` screens needs the navigation convention plugin.
- The `moduleName` KSP argument is typically set to `project.name` in the convention plugin.

### Rules
- Check if the project has a navigation convention plugin before manually adding navigation dependencies.
- If creating a new feature module with screens, apply the navigation convention plugin in `build.gradle.kts`.
