# Rule 12: Conventional Commits

> Read this when writing commit messages or generating changelogs.

---

## 1. Format

Every commit message **must** follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<optional scope>): <short description>

<optional body>

<optional footer(s)>
```

### Examples
```
feat(player): add picture-in-picture support
fix(auth): prevent crash on expired token refresh
refactor(home): extract catalog loading into use case
docs: update README with build instructions
chore(deps): bump compose-bom to 2026.02.01
test(profile): add ViewModel unit tests for edit flow
```

---

## 2. Types

| Type | When to Use |
|---|---|
| `feat` | A new feature or user-facing behavior. |
| `fix` | A bug fix. |
| `refactor` | Code restructuring with no behavior change. |
| `docs` | Documentation-only changes (README, KDoc, comments). |
| `style` | Formatting, whitespace, missing semicolons — no logic change. |
| `test` | Adding or updating tests — no production code change. |
| `chore` | Build scripts, CI config, dependency bumps, tooling. |
| `perf` | Performance improvements with no behavior change. |
| `ci` | CI/CD pipeline changes (GitHub Actions, workflows). |
| `build` | Build system or external dependency changes (Gradle, version catalog). |
| `revert` | Reverting a previous commit. Reference the reverted hash in the body. |

---

## 3. Scope

- The scope is **optional** but encouraged. It identifies the module, feature, or area affected.
- Use the module name or feature name in lowercase: `player`, `home`, `auth`, `database`, `gradle`, `deps`.
- If a change spans multiple modules, either omit the scope or use a general term like `core`.

---

## 4. Description Rules

- **Lowercase** — do not capitalize the first letter of the description.
- **Imperative mood** — write as a command: "add", "fix", "remove", not "added", "fixes", "removed".
- **No trailing period** — the subject line does not end with `.`
- **≤ 72 characters** — keep the full subject line (type + scope + description) under 72 characters.

```
// ✅ CORRECT
feat(search): add genre filter chips

// ❌ WRONG — past tense, capitalized, trailing period
feat(search): Added genre filter chips.
```

---

## 5. Body (Optional)

- Separate from the subject with a blank line.
- Use the body to explain **what** and **why**, not **how** (the diff shows how).
- Wrap at 72 characters per line.

```
fix(player): prevent seek bar from resetting on rotation

The seek position was not saved in SavedStateHandle before the
activity was recreated. Now the current position is persisted
across configuration changes.
```

---

## 6. Breaking Changes

- Add `!` after the type/scope **and/or** a `BREAKING CHANGE:` footer.
- Both methods are valid; using both together is preferred for visibility.

```
feat(database)!: migrate to new schema v5

BREAKING CHANGE: Removes the `watchlist` table. Users must re-add
their saved items after upgrading.
```

---

## 7. Multi-Change Commits

- **Prefer atomic commits** — one logical change per commit.
- If a single commit must cover multiple changes (e.g., a refactor that touches several modules), list them in the body:

```
refactor(core): unify error handling across data layer

- Replace Resource with Outcome in all repositories
- Update all use cases to return Outcome
- Adjust ViewModel error mapping
```

---

## 8. Anti-Patterns

| ❌ Don't | ✅ Do Instead |
|---|---|
| `fix: stuff` | `fix(auth): handle null token on refresh` |
| `update code` | `refactor(home): extract carousel into composable` |
| `WIP` | Don't commit WIP to shared branches; use drafts or stash. |
| `fix: Fix the bug` | `fix(player): prevent crash on null media source` |
| `feat: Add feature and fix bug` | Split into two commits: one `feat`, one `fix`. |
