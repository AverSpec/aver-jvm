# Aver for JVM (Kotlin)

Domain-driven acceptance testing for Kotlin and the JVM.

## Install

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("dev.averspec:averspec:0.1.0")
}
```

## Quick Example

```kotlin
import dev.averspec.*
import org.junit.jupiter.api.Test

// Domain
val d = Domain("task-board")
val createTask = d.action<Map<String, String>>("create_task")
val taskInStatus = d.assertion<Map<String, String>>("task_in_status")

// Adapter
val adapter = buildAdapter(d, UnitProtocol { mutableMapOf<String, String>() }) {
    handle(createTask) { board, p -> board[p["title"]!!] = "backlog" }
    handle(taskInStatus) { board, p ->
        assert(board[p["title"]!!] == p["status"]!!)
    }
}

val s = suite(d, adapter)

// Tests
class TaskBoardTest {
    @Test
    fun `create task with default status`() {
        s.test("create task with default status") { ctx ->
            ctx.given(createTask, mapOf("title" to "Fix bug"))
            ctx.then(taskInStatus, mapOf("title" to "Fix bug", "status" to "backlog"))
        }
    }
}
```

Run with Gradle:

```bash
./gradlew test
```

## Links

- [Documentation](https://averspec.dev)
- [Architecture and methodology](https://github.com/AverSpec/aver)
