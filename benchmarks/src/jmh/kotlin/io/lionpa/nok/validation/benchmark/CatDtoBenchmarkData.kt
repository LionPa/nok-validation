package io.lionpa.nok.validation.benchmark

import kotlin.random.Random

object CatDtoBenchmarkData {

    private val validNames = listOf(
        "meowMilo",
        "meowLuna",
        "meowNala",
        "meowOscar",
        "meowSimba"
    )

    private val invalidNames = listOf(
        "Milo",
        "Luna",
        "Oscar",
        "cat",
        "verylongcatname"
    )

    private val validToys = listOf(
        "ball42",
        "mouse42",
        "feather42",
        "laser42",
        "spring42"
    )

    private val invalidToys = listOf(
        "ball",
        "mouse",
        "feather",
        "laser",
        ""
    )

    fun <T> createValid(
        randomize: Boolean = false,
        factory: (String, List<String>) -> T
    ): T {
        val name = if (randomize) {
            validNames.random()
        } else {
            validNames.first()
        }

        val favoriteToys = List(
            if (randomize) Random.nextInt(1, 6) else 3
        ) {
            if (randomize) validToys.random() else validToys.first()
        }

        return factory(name, favoriteToys)
    }

    fun <T> createInvalid(
        brokenName: Boolean = true,
        brokenList: Boolean = true,
        randomize: Boolean = false,
        factory: (String, List<String>) -> T
    ): T {
        val name = if (brokenName) {
            if (randomize) invalidNames.random() else invalidNames.first()
        } else {
            if (randomize) validNames.random() else validNames.first()
        }

        val favoriteToys = if (brokenList) {
            val size = if (randomize) Random.nextInt(1, 6) else 3

            val toys = MutableList(size) {
                if (randomize) validToys.random() else validToys.first()
            }

            toys[if (randomize) Random.nextInt(toys.size) else 0] =
                if (randomize) invalidToys.random() else invalidToys.first()

            toys
        } else {
            List(
                if (randomize) Random.nextInt(1, 6) else 3
            ) {
                if (randomize) validToys.random() else validToys.first()
            }
        }

        return factory(name, favoriteToys)
    }
}