# Rule 05: Compose UI

> Read this when writing or editing any Jetpack Compose code — screens, components, previews, modifiers, state management.

---

## 1. Core UI Packages

The standard Compose stack for Android projects. Verify availability against the project's version catalog before using.

| Package | Purpose |
|---|---|
| Compose BOM | Version-aligned Compose dependencies |
| Material 3 | UI components and theming |
| Compose Foundation | Layout primitives, gestures, scrolling |
| Compose Animation | Animated content, transitions |
| Coil Compose | Image loading |
| Accompanist | Permissions, system UI |
| Hilt Navigation Compose | ViewModel injection in nav graphs |
| Compose Adaptive | Adaptive layouts for different screen sizes |
| Kotlinx Immutable Collections | `PersistentList`, `PersistentMap`, `PersistentSet` for stable parameters |

---

## 2. Custom Composables

### Before Creating Any Reusable Composable
1. **Ask the developer** if they want Material 3 defaults or custom composables for the app.
2. **Ask the developer to provide the path** to the folder containing existing custom composables (e.g., `core/presentation/common/`, `core/presentation/mobile/`).
3. **Search that directory** for an existing equivalent before creating a new one.
4. If a match or near-match exists, **use it** — or ask the developer if they want to extend it rather than duplicate it.

### Why This Matters
Projects often have custom composables (buttons, top bars, bottom sheets, cards) that maintain design consistency. Creating duplicates fragments the design system and causes visual inconsistencies.

---

## 3. Composable Structure Rules

### Visibility Priority
`private` > `internal` > `public`

- **Private:** Sub-composables within a file. Default choice.
- **Internal:** Content composables used only within the module (e.g., screen content functions).
- **Public:** Only `@Destination`-annotated composables (or composables exported for cross-module use).

### Private Sub-Composables Stay in the File
If a composable is `private` and only used within one file, keep it in that file. Only extract to a separate file when:
- The file exceeds **~500 lines** (excluding preview code).
- The composable is reused across multiple files within the module.

### Screen Composable Pattern (Three-Layer Structure)

Every screen MUST follow this three-layer pattern:

```kotlin
// LAYER 1: Public entry point — @Destination annotated
// This is the only public composable. It creates the ViewModel and delegates.
@Destination<ExternalModuleGraph>
@Composable
fun FeatureScreen(
    navigator: FeatureScreenNavigator,
    // ... navigation args, result recipients
) {
    FeatureScreen(
        navigator = navigator,
        viewModel = hiltViewModel(),
    )
}

// LAYER 2: Internal bridge — receives ViewModel, collects state, hoists callbacks
// This layer exists to separate ViewModel from pure UI for testability.
@Composable
internal fun FeatureScreen(
    navigator: FeatureScreenNavigator,
    viewModel: FeatureViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Collect other flows...

    FeatureScreenContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = navigator::goBack,
        // ... all callbacks hoisted
    )
}

// LAYER 3: Private content — pure UI, no ViewModel, used in @Preview
// This is where all the actual UI lives.
@Composable
private fun FeatureScreenContent(
    uiState: FeatureUiState,
    onAction: (Action) -> Unit,
    onNavigateBack: () -> Unit,
    // ... all state and callbacks as parameters
) {
    // UI implementation
}
```

### Why Three Layers
- **Layer 1** satisfies the navigation framework.
- **Layer 2** bridges the ViewModel to the UI and can be replaced in tests.
- **Layer 3** is a pure function of its parameters — fully previewable, fully testable without DI.

---

## 4. Preview Code Rules

### Mandatory Adaptive Previews
All preview code MUST include different device configurations to verify adaptiveness. Define a base preview composable, then reference it from device-specific previews:

```kotlin
@Preview
@Composable
private fun FeatureScreenBasePreview() {
    // Create mock state internally
    var uiState by remember { mutableStateOf(FeatureUiState()) }

    // Wrap with the project's theme and Surface
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            FeatureScreenContent(
                uiState = uiState,
                onAction = { /* update mock state as needed */ },
                onNavigateBack = {},
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun FeatureScreenCompactLandscapePreview() {
    FeatureScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun FeatureScreenMediumPortraitPreview() {
    FeatureScreenBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun FeatureScreenMediumLandscapePreview() {
    FeatureScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun FeatureScreenExtendedPortraitPreview() {
    FeatureScreenBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun FeatureScreenExtendedLandscapePreview() {
    FeatureScreenBasePreview()
}
```

### Rules
- Base preview creates mock state and wraps with theme + `Surface`.
- Device-specific previews simply call the base preview — no duplicated logic.
- Navigator/callback parameters use no-op lambdas in previews.
- **Check the project for its theme composable name** (e.g., `FlixclusiveTheme`, `AppTheme`, `MyAppTheme`) — don't guess.

---

## 5. Compose Best Practices

### 5.1. State Hoisting
**Always hoist `State<T>`.** Content composables receive state as parameters and emit events via callbacks. They never create or own the state they display.

```kotlin
// ✅ State hoisted — content receives state, emits events
@Composable
private fun Counter(count: Int, onIncrement: () -> Unit) {
    Button(onClick = onIncrement) { Text("Count: $count") }
}

// ❌ State not hoisted — content owns its own state
@Composable
private fun Counter() {
    var count by remember { mutableStateOf(0) }
    Button(onClick = { count++ }) { Text("Count: $count") }
}
```

### 5.2. State Deferral
**Defer state reads** when the state may change frequently. Use lambda-based parameters to push the read into the composition phase where it's needed, avoiding unnecessary recompositions of parent composables.

```kotlin
// ✅ Deferred — parent doesn't recompose when scrollOffset changes
@Composable
private fun Header(scrollOffset: () -> Float) {
    val offset = scrollOffset()
    // use offset
}

// ❌ Not deferred — parent recomposes on every scroll
@Composable
private fun Header(scrollOffset: Float) {
    // use scrollOffset
}
```

### 5.3. Callback Naming
Callback parameters MUST use **present tense**, never past tense:

```kotlin
// ✅ CORRECT — present tense
onTouch: () -> Unit
onHoverProfile: (User) -> Unit
onConsumeError: () -> Unit
onChange: (String) -> Unit

// ❌ WRONG — past tense
onTouched: () -> Unit
onProfileHovered: (User) -> Unit
onErrorConsumed: () -> Unit
onChanged: (String) -> Unit
```

### 5.4. Modifier Lambda Usage
Modifiers that have lambda versions **MUST use the lambda version when the state they read changes**, regardless of change frequency. If the modifier feeds on an immutable/hardcoded value, the non-lambda version is correct.

```kotlin
// ✅ Immutable value — non-lambda is correct
Modifier.offset(x = 16.dp, y = 0.dp)

// ✅ Changing state — lambda version is correct
Modifier.offset { IntOffset(animatedX.roundToInt(), 0) }

// ❌ Changing state — non-lambda is WRONG (causes unnecessary recomposition)
Modifier.offset(x = animatedX.dp, y = 0.dp)
```

This applies to: `.offset {}`, `.graphicsLayer {}`, `.drawBehind {}`, `.drawWithContent {}`, `.padding {}` (when applicable), and any other modifier with a lambda variant.

### 5.5. Stability Annotations
- All `data class`es used at the UI level MUST be marked `@Stable` or `@Immutable` as much as possible.
- All `*UiState(...)` data classes MUST be annotated — `@Stable` is the safer default; use `@Immutable` only when every property is truly immutable.
- **If a data class is in a module that does not have Compose in its Gradle dependencies, do NOT add Compose annotations.** Keep it as-is.

```kotlin
// ✅ UI state annotated
@Stable
internal data class HomeUiState(
    val isLoading: Boolean = false,
    val items: PersistentList<Item> = persistentListOf(),
)

// ✅ Immutable — all properties are val and immutable types
@Immutable
internal data class ProfilesScreenUiState(
    val isLoggedIn: Boolean = false,
    val focusedProfile: User? = null,
)
```

### 5.6. StateFlow over MutableState in ViewModels
- ViewModels expose state via `StateFlow` (`MutableStateFlow` + `.asStateFlow()`).
- Composables collect via `collectAsStateWithLifecycle()`.
- This keeps ViewModels free of Compose dependencies and makes them unit-testable with Turbine.

### 5.7. Recomposition Audit
Before finalizing any Compose code, validate:

- [ ] No `remember {}` blocks with missing keys that should cause invalidation.
- [ ] No `LaunchedEffect` with overly broad keys (e.g., `Unit` when a specific trigger exists).
- [ ] No state reads in a composable body that should be deferred via lambda.

### 5.8. Performance Reference
Follow: https://developer.android.com/develop/ui/compose/performance/bestpractices
