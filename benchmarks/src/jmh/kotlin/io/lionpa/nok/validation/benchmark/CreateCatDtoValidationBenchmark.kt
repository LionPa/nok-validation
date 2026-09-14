package io.lionpa.nok.validation.benchmark

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 300, time = 20, timeUnit = TimeUnit.MILLISECONDS)
 @Measurement(iterations = 200, time = 20, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
open class CreateCatDtoValidationBenchmark {

    companion object {
        @JvmStatic
        val VALIDATE_HANDLE: MethodHandle =
            MethodHandles.lookup()
                .unreflect(
                    CreateCatDto::class.java.getMethod("generated\$validate")
                )
    }

    private lateinit var validDto: CreateCatDto
    private lateinit var invalidDto: CreateCatDto

    private lateinit var mixedDtos: Array<CreateCatDto>
    private lateinit var rareDtos: Array<CreateCatDto>

    private var mixedIndex = 0
    private var rareIndex = 0

    @Setup(Level.Trial)
    fun setup() {
        validDto = CatDtoGenerator.createValid(randomize = true)
        invalidDto = CatDtoGenerator.createInvalid(randomize = true)

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
            VALIDATE_HANDLE.invokeExact(validDto) as List<*>
        )
    }

    @Benchmark
    fun benchmarkInvalidDto(blackhole: Blackhole) {
        blackhole.consume(
            VALIDATE_HANDLE.invokeExact(invalidDto) as List<*>
        )
    }

    @Benchmark
    fun benchmarkMixedDto(blackhole: Blackhole) {
        val dto = mixedDtos[mixedIndex]
        mixedIndex = (mixedIndex + 1) % mixedDtos.size

        blackhole.consume(
            VALIDATE_HANDLE.invokeExact(dto) as List<*>
        )
    }

    @Benchmark
    fun benchmarkRareDto(blackhole: Blackhole) {
        val dto = rareDtos[rareIndex]
        rareIndex = (rareIndex + 1) % rareDtos.size

        blackhole.consume(
            VALIDATE_HANDLE.invokeExact(dto) as List<*>
        )
    }
}