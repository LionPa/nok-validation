# Nok Validation Spring Integration

Spring Framework and Spring Boot integration for Nok Validation.

## Installation

```kotlin
plugins {
    id("io.github.lionpa.nok-validation") version "1.0.1"
}

dependencies {
    implementation("io.github.lionpa:nok-validation-spring:1.0.1")
}
```

## Usage

Use `@Validated` (or `@Valid`) on `@Validatable` request bodies in your controllers:

```kotlin
@RestController
@RequestMapping("/cats")
class CatController {

    @PostMapping
    fun createCat(@Validated @RequestBody dto: CreateCatDto): CreateCatDto {
        return dto
    }
}
```
