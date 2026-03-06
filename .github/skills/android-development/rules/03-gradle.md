# Rule 03: Gradle & Dependency Management

> Read this when touching `build.gradle.kts`, adding dependencies, creating modules, or setting up build-logic.

---

## 1. Version Catalog is Mandatory

All dependencies, versions, and plugins MUST be declared in `gradle/libs.versions.toml`.

### Rules
- **NEVER** hardcode a dependency version directly in a `build.gradle.kts` file.
- **ALWAYS** reference through the catalog: `libs.some.library`, `libs.plugins.some.plugin`.
- Before adding a new dependency, **search the version catalog first** to check if it already exists. Duplicate entries cause conflicts.

### How to Add a New Dependency
1. Add the version to `[versions]` if it doesn't exist.
2. Add the library to `[libraries]` referencing the version.
3. If it's a plugin, add to `[plugins]`.
4. Use it in the module's `build.gradle.kts` via `libs.<alias>` or `libs.plugins.<alias>`.

### Example
```toml
# In gradle/libs.versions.toml
[versions]
someLibrary = "1.2.3"

[libraries]
some-library = { group = "com.example", name = "some-library", version.ref = "someLibrary" }
```
```kotlin
// In module's build.gradle.kts
dependencies {
    implementation(libs.some.library)
}
```

---

## 2. Convention Plugins (Build-Logic)

For **multi-module projects**, convention plugins in `build-logic/` centralize shared Gradle configuration. Each plugin encapsulates a reusable set of plugins, dependencies, and settings.

### Rules
- **Multi-module projects:** Always use convention plugins. Never duplicate Gradle configuration across modules.
- **Single-module projects:** No build-logic needed — use standard project structure.
- When creating a new module, **check which convention plugins already exist** in `build-logic/convention/` before writing custom Gradle config.

### How to Discover Convention Plugins
1. Read `build-logic/convention/build.gradle.kts` — the `gradlePlugin {}` block lists all registered plugins with their IDs.
2. Read each plugin's implementation class to understand what it applies.
3. Check `gradle/libs.versions.toml` under `[plugins]` for the convention plugin IDs (they usually have `version = "unspecified"`).

### What Convention Plugins Typically Cover
- Android library base setup (Kotlin, desugaring, linting)
- Compose compiler + BOM
- Hilt + KSP
- Navigation library (e.g., Compose Destinations)
- Feature module setup (library + hilt + lifecycle + presentation deps)
- Room + KSP
- Testing stack (assertion lib, mocking lib, flow testing, coroutines test)

---

## 3. Feature Module `build.gradle.kts` Pattern

When creating a new feature module:

1. **Check existing feature modules** for the exact plugin and dependency pattern.
2. Apply convention plugins via `alias(libs.plugins.<convention>)`.
3. Set the `android.namespace` to match the module's package.
4. Declare only the module-specific dependencies — convention plugins handle shared deps.

### Template (Adapt to Project)
```kotlin
plugins {
    alias(libs.plugins.<project>.feature.mobile)  // or equivalent convention plugin
    alias(libs.plugins.<project>.compose)
    alias(libs.plugins.<project>.testing)
    // Add navigation plugin if this module has @Destination screens
}

android {
    namespace = "<base.package>.<layer>.<module_name>"
}

dependencies {
    // Project modules — only what this module directly needs
    implementation(projects.coreCommon)
    implementation(projects.coreDatabase)
    // ...

    // External libraries from version catalog
    implementation(libs.compose.material3)
    // ...

    // Test dependencies
    testImplementation(projects.coreTesting)
}
```

### Rules
- **Use typesafe project accessors** (e.g., `projects.coreCommon`) if the project has `TYPESAFE_PROJECT_ACCESSORS` enabled. Verify in `settings.gradle.kts`.
- **Never add a dependency to a feature module that isn't needed.** Each module should declare the minimum set of dependencies it requires.

---

## 4. Adding Dependencies for New Requirements

When a task requires a library that isn't already in the project:

### Step-by-Step
1. **Search** `gradle/libs.versions.toml` to confirm it doesn't already exist.
2. **Add** the version, library (and plugin if applicable) to `libs.versions.toml`.
3. **Add** the dependency to the target module's `build.gradle.kts`.
4. If the dependency is needed across many modules, consider adding it to a convention plugin instead.

### Ask the Developer
If you're unsure whether a new dependency should be introduced (e.g., adding a whole new library like Arrow, Ktor, or Paging), **ask the developer first**. New library introductions are architectural decisions.

---

## 5. Module Creation Checklist

When creating a new module:

1. Add `include(":module-path")` to `settings.gradle.kts`.
2. If the project uses directory flattening (e.g., mapping `data-provider` → `data/provider`), follow the same pattern.
3. Create the module directory with `build.gradle.kts`, `src/main/kotlin/`, and `src/test/kotlin/`.
4. Apply the appropriate convention plugins.
5. Set the correct `android.namespace`.
6. Add the module to any parent module's dependencies if needed.
