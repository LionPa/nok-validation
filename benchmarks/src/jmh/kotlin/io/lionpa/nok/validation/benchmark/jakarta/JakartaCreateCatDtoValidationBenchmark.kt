package io.lionpa.nok.validation.benchmark.jakarta

import io.lionpa.nok.validation.benchmark.CatDtoBenchmarkData
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(
    iterations = 300,
    time = 20,
    timeUnit = TimeUnit.MILLISECONDS
)
@Measurement(
    iterations = 200,
    time = 20,
    timeUnit = TimeUnit.MILLISECONDS
)
@Fork(3)
open class JakartaCreateCatDtoValidationBenchmark {

    private lateinit var validator: Validator

    private lateinit var validDto: CreateCatDto
    private lateinit var invalidDto: CreateCatDto

    private lateinit var mixedDtos: Array<CreateCatDto>
    private lateinit var rareDtos: Array<CreateCatDto>

    private var mixedIndex = 0
    private var rareIndex = 0

    @Setup(Level.Trial)
    fun setup() {
        validator = Validation
            .buildDefaultValidatorFactory()
            .validator

        validDto = CatDtoBenchmarkData.createValid(
            randomize = true,
            factory = ::CreateCatDto
        )

        invalidDto = CatDtoBenchmarkData.createInvalid(
            randomize = true,
            factory = ::CreateCatDto
        )

        mixedDtos = arrayOf(
            validDto,
            validDto,
            validDto,
            validDto,
            invalidDto
        )

        rareDtos = Array(100) { index ->
            if (index == 0) invalidDto else validDto
        }
    }

    @Benchmark
    fun benchmarkValidDto(blackhole: Blackhole) {
        blackhole.consume(
            validator.validate(validDto)
        )
    }

    @Benchmark
    fun benchmarkInvalidDto(blackhole: Blackhole) {
        blackhole.consume(
            validator.validate(invalidDto)
        )
    }

    @Benchmark
    fun benchmarkMixedDto(blackhole: Blackhole) {
        val dto = mixedDtos[mixedIndex]

        mixedIndex++
        if (mixedIndex == mixedDtos.size) {
            mixedIndex = 0
        }

        blackhole.consume(
            validator.validate(dto)
        )
    }

    @Benchmark
    fun benchmarkRareDto(blackhole: Blackhole) {
        val dto = rareDtos[rareIndex]

        rareIndex++
        if (rareIndex == rareDtos.size) {
            rareIndex = 0
        }

        blackhole.consume(
            validator.validate(dto)
        )
    }
}