# Rule 01: Anti-Hallucination

> **Priority: CRITICAL.** These rules are mandatory and override all other instructions. Violating any of these results in broken, non-compiling code. Read this file before every task.

---

## 1. Never Invent Symbols

**NEVER invent or hallucinate class names, package paths, function signatures, variable names, or dependency coordinates.**

Before generating code that references any project-internal symbol (class, interface, use case, repository, entity, extension function, utility, constant), you MUST:

1. **Search** the project for the symbol by name.
2. **Read** the actual source file to confirm it exists.
3. **Verify** its exact signature (parameters, return type, visibility, package).

If the symbol does not exist, do NOT use it. Either create it (if appropriate for the task) or ask the developer.

### Why This Matters
LLMs frequently generate plausible-looking but fictional class names (e.g., `AppNavigator`, `NetworkHelper`, `StringUtils`) that do not exist in the project. This results in unresolvable imports and compilation errors.

---

## 2. Never Assume Dependencies

**NEVER assume a library or dependency is available in a module.**

Before importing or using any external library:

1. **Read** `gradle/libs.versions.toml` (the version catalog) to confirm the dependency exists.
2. **Read** the target module's `build.gradle.kts` to confirm the dependency is declared there.

If the dependency is in the version catalog but not in the module's `build.gradle.kts`, you must add it to the module's dependencies. If the dependency is not in the version catalog at all, add it to `libs.versions.toml` first, then to the module.

### Common Mistakes to Avoid
- Assuming `kotlinx-collections-immutable` is available in every module (it's not — check first).
- Assuming Compose dependencies are available in `data/` or `domain/` modules (they usually are not).
- Using `libs.` references that don't exist in the catalog.

---

## 3. Never Guess Compose APIs

**NEVER guess Compose APIs, Modifier functions, or their signatures.**

If you are unsure whether a Compose API exists or what its parameters are:

1. **Search** the project for existing usage of that API.
2. If no usage exists, **do not assume it exists**. Stick to well-known, stable Compose APIs.

### Common Mistakes to Avoid
- Inventing Modifier extension functions (e.g., `Modifier.shimmer()`, `Modifier.gradientBackground()`) that don't exist in the project.
- Using Compose APIs from experimental or removed packages without verifying availability.
- Confusing Material 2 and Material 3 APIs (e.g., `androidx.compose.material.Text` vs `androidx.compose.material3.Text`).

---

## 4. Never Assume Module Names

**NEVER assume module names or typesafe project accessor names.**

Always verify against `settings.gradle.kts`:

- Projects using `TYPESAFE_PROJECT_ACCESSORS` reference modules like `projects.coreDatabase` in dependency blocks.
- The raw include format in `settings.gradle.kts` may differ (e.g., `include(":core-database")`).
- Never guess the accessor format — read `settings.gradle.kts` to confirm.

### How to Verify
1. Read `settings.gradle.kts` to find all `include()` statements.
2. Check if the project uses `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`.
3. Derive the correct accessor name from the include path (e.g., `:core-database` → `projects.coreDatabase`).

---

## 5. Always Read Before Editing

**When editing existing code, ALWAYS read the file first.**

Before making any change to an existing file:

1. **Read the full file** (or at minimum the relevant sections) to understand its current state.
2. **Study existing imports** — follow the same import style and aliasing conventions.
3. **Study existing patterns** — match the code style, naming, visibility modifiers, and structure already in use.

Never make blind edits based on assumptions about a file's content.

---

## 6. Always Study a Reference Before Creating

**When creating a feature from scratch, ALWAYS read an existing similar feature module first.**

Before generating a new feature, find and read a complete existing feature in the project. Study:

- Its `build.gradle.kts` (plugins, dependencies)
- Its ViewModel (state management, injection, patterns)
- Its Screen composable (structure, previews, state hoisting)
- Its Navigator interface (composition of action interfaces)
- Its UI state model (annotations, placement)
- Its DI module (if any)
- Its tests (test structure, mocking, assertions)

This is non-negotiable. The reference feature is the source of truth for the project's conventions.

### How to Find a Reference
Ask the developer: *"Which existing feature module should I use as a reference?"* If they don't specify, search for a feature module that has a ViewModel, screen, tests, and navigator — then use it.

---

## 7. Validate Dependency Graph on Every Edit

**When editing a file, validate that your changes do not violate the module's dependency graph.**

After generating or editing code in any file:

1. **Read** the module's `build.gradle.kts` to see its declared dependencies.
2. **Verify** that every import in your edited file references a package from a module in that dependency list (or from the module itself, or from the Kotlin/Android stdlib).
3. **NEVER** introduce upward or circular dependencies:
   - A `core/` module MUST NOT import from `data/`, `domain/`, or `feature/`.
   - A `data/` module MUST NOT import from `domain/` or `feature/`.
   - A `domain/` module MUST NOT import from `feature/`.

### How to Validate
For each import you add, trace it back to the module that provides it. Confirm that module appears in the current module's `build.gradle.kts` under `implementation(...)`, `api(...)`, or `testImplementation(...)`.
