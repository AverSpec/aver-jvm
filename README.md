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

// 1. Define a domain with typed markers
val d = domain("task-board") {
    action<String>("add task")
    assertion<Int>("has total tasks")
}

@Suppress("UNCHECKED_CAST")
val addTask = d.markers["add task"] as ActionMarker<String>
@Suppress("UNCHECKED_CAST")
val hasTotalTasks = d.markers["has total tasks"] as AssertionMarker<Int>

// 2. Implement an adapter that maps markers to real code
val adapter = implement<MutableList<String>>(d, UnitProtocol { mutableListOf() }) {
    onAction(addTask) { board, title -> board.add(title) }
    onAssertion(hasTotalTasks) { board, expected ->
        if (board.size != expected) throw AssertionError("Expected $expected tasks, got ${board.size}")
    }
}

// 3. Create a suite and write tests
val s = suite(d, adapter)

class TaskBoardTest {
    @Test
    fun `add a task to the board`() = s.run { ctx ->
        ctx.Given(addTask, "Fix bug")
        ctx.Then(hasTotalTasks, 1)
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
