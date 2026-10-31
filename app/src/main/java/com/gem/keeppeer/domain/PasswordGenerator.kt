package com.gem.keeppeer.domain

import java.security.SecureRandom

data class GeneratorOptions(
    val length: Int = 20,
    val lowercase: Boolean = true,
    val uppercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = true,
)

/**
 * Single policy definition. ASCII pools are distinct. Ambiguity is a documented
 * visual convenience, not a security rating. No options or candidates are persisted.
 * One uniform bounded draw per required pool, then uniform draws from their union,
 * followed by secure Fisher-Yates. Guarantees coverage; does NOT claim uniformity
 * over all strings satisfying coverage.
 */
object GeneratorPolicy {
    const val MIN_LENGTH = 8
    const val MAX_LENGTH = 128
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val DIGITS = "0123456789"
    const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"
    const val AMBIGUOUS = "Il1O0o|"

    fun pools(options: GeneratorOptions): List<String> {
        require(options.length in MIN_LENGTH..MAX_LENGTH) { "Invalid generator length" }
        val pools = listOf(
            options.lowercase to LOWERCASE, options.uppercase to UPPERCASE,
            options.digits to DIGITS, options.symbols to SYMBOLS,
        ).filter { it.first }.map { (_, pool) ->
            if (options.excludeAmbiguous) pool.filterNot { it in AMBIGUOUS } else pool
        }
        require(pools.isNotEmpty() && pools.all { it.isNotEmpty() }) { "Invalid generator classes" }
        require(options.length >= pools.size) { "Insufficient generator length" }
        check(pools.joinToString("").toSet().size == pools.sumOf { it.length })
        return pools
    }
}

class PasswordGenerator private constructor(private val nextIndex: (Int) -> Int) {
    // SecureRandom construction and nextInt failures propagate; there is no fallback.
    constructor() : this(SecureRandom()::nextInt)
    internal companion object {
        /** Tests inject bounded draws only; production has no seed or fallback API. */
        fun forTest(nextIndex: (Int) -> Int) = PasswordGenerator(nextIndex)
    }

    fun generate(options: GeneratorOptions = GeneratorOptions()): String {
        val pools = GeneratorPolicy.pools(options)
        val all = pools.joinToString("")
        fun draw(bound: Int): Int = nextIndex(bound).also { check(it in 0 until bound) }
        val output = CharArray(options.length)
        try {
            pools.forEachIndexed { index, pool -> output[index] = pool[draw(pool.length)] }
            for (index in pools.size until output.size) output[index] = all[draw(all.length)]
            for (index in output.lastIndex downTo 1) {
                val other = draw(index + 1)
                val value = output[index]; output[index] = output[other]; output[other] = value
            }
            return String(output)
        } finally { output.fill('\u0000') }
    }
}
