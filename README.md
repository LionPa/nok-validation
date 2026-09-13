# Nok Validation

> A high-performance, type-safe, and ergonomic compile-time validation library for Kotlin.

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=flat-square\&logo=kotlin\&logoColor=white)
![Maven Central Version](https://img.shields.io/maven-central/v/io.github.lionpa.nok-validation/io.github.lionpa.nok-validation.gradle.plugin?style=flat-square)

Nok Validation is a compile-time validation library for Kotlin. It can be used standalone, integrated with backend frameworks, or used as part of the Nok Framework.

## Features

* Compile-time validation
* Type-safe validators
* Kotlin DSL for defining validation rules
* Built-in validators
* Custom validators
* Framework-independent

## Example

Validation rules are defined alongside the fields they apply to:

```kotlin
@Validatable
data class CreateCatDto(

    val name: String = schema {
        isNotBlank()
        length(3, 10)
        contains("meow")
    },

    val favoriteToys: List<String> = schema {
        forEach {
            contains("42")
        }
    }
)
```

The schema is intentionally kept short and declarative. Validation logic belongs in validators rather than directly in the schema.

Validation rules are processed by the Kotlin compiler and added to the generated class.

## Installation

### Gradle Kotlin DSL

```kotlin
plugins {
    id("io.github.lionpa.nok-validation") version "1.0.1"
}
```

## Built-in Validators

Nok Validation provides a standard set of commonly used validators.

### String validation

```kotlin
val name: String = schema {
    isNotBlank()
    length(3, 10)
    contains("meow")
    regex(Regex("[a-zA-Z]+"))
}
```

Available string validators include:

* `isNotBlank()`
* `length(min, max)`
* `length(range)`
* `contains(value)`
* `regex(regex)`

### Collections

Collection elements can be validated using `forEach`:

```kotlin
val favoriteToys: List<String> = schema {
    forEach {
        contains("42")
    }
}
```

### Nullability

Generic validators can be applied to different types:

```kotlin
val value: String? = schema {
    isNotNull()
}
```

## Custom Validators

Custom validators are defined as Kotlin extension functions annotated with `@Validator`:

```kotlin
@Validator
inline fun Schema<String>.contains(value: String) {
    if (!this.value.contains(value)) {
        invalidMessages.add(
            "$fieldName does not contain $value"
        )
    }
}
```

Validators can be specific to a type:

```kotlin
@Validator
inline fun Schema<String>.isNotBlank() {
    if (value.isBlank()) {
        invalidMessages.add("$fieldName is blank!")
    }
}
```

Or generic:

```kotlin
@Validator
inline fun <T> Schema<T>.isNotNull() {
    if (value == null) {
        invalidMessages.add("$fieldName is null!")
    }
}
```

Custom validators can contain arbitrary Kotlin logic. Keeping that logic in named validators allows schemas to remain concise and declarative.

For example:

```kotlin
val name: String = schema {
    isNotBlank()
    length(3, 10)
    contains("meow")
}
```

instead of putting the validation logic directly inside the schema.

## Compile-time Validation

Nok Validation uses a Kotlin compiler plugin to process `@Validatable` classes at compile time.

The validation rules from the schema are compiled into validation logic that is added to the generated class. No separate validator class needs to be written.

## Performance & Benchmarks

Nok Validation is designed with performance in mind.

Benchmarks, sources, and decompiled examples will be added to the `benchmarks` directory.

## License

Licensed under the [Apache License 2.0](LICENSE).
