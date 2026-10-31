package com.gem.keeppeer

import com.gem.keeppeer.domain.*
import org.junit.Assert.*
import org.junit.Test

class PasswordGeneratorTest {
    @Test fun defaultsBoundsAndDistinctExactPools() {
        assertEquals(GeneratorOptions(20, true, true, true, true, true), GeneratorOptions())
        assertEquals(8, GeneratorPolicy.MIN_LENGTH)
        assertEquals(128, GeneratorPolicy.MAX_LENGTH)
        assertEquals("abcdefghijklmnopqrstuvwxyz", GeneratorPolicy.LOWERCASE)
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", GeneratorPolicy.UPPERCASE)
        assertEquals("0123456789", GeneratorPolicy.DIGITS)
        assertEquals("!@#$%^&*()-_=+[]{};:,.?/", GeneratorPolicy.SYMBOLS)
        assertEquals("Il1O0o|", GeneratorPolicy.AMBIGUOUS)
        val all = GeneratorPolicy.pools(GeneratorOptions(excludeAmbiguous = false)).joinToString("")
        assertEquals(all.length, all.toSet().size)
    }
    @Test fun allConfigurationsHaveExactLengthAllowedCharactersAndRequiredClasses() {
        val generator = PasswordGenerator()
        for (mask in 1..15) for (exclude in listOf(false, true)) for (length in listOf(8, 20, 128)) {
            val options = GeneratorOptions(length, mask and 1 != 0, mask and 2 != 0,
                mask and 4 != 0, mask and 8 != 0, exclude)
            val pools = GeneratorPolicy.pools(options)
            repeat(3) {
                val value = generator.generate(options)
                // Assert properties without echoing candidates in assertion messages.
                assertTrue(value.length == length)
                assertTrue(value.all { it in pools.joinToString("") })
                assertTrue(pools.all { pool -> value.any { it in pool } })
                if (exclude) assertTrue(value.none { it in GeneratorPolicy.AMBIGUOUS })
            }
        }
    }
    @Test fun invalidEmptyAndImpossibleOptionsRejectBeforeRandomDraws() {
        var draws = 0
        val generator = PasswordGenerator.forTest { draws++; 0 }
        for (options in listOf(
            GeneratorOptions(length = 7), GeneratorOptions(length = 129),
            GeneratorOptions(length = 0), GeneratorOptions(length = 3),
            GeneratorOptions(lowercase = false, uppercase = false, digits = false, symbols = false),
        )) assertThrows(IllegalArgumentException::class.java) { generator.generate(options) }
        assertEquals(0, draws)
        // Fixed policy never has an empty selected effective class, even after ambiguity exclusion.
        assertTrue(GeneratorPolicy.pools(GeneratorOptions()).all { it.isNotEmpty() })
    }
    @Test fun boundedDrawsAndFisherYatesAreUsed() {
        val bounds = mutableListOf<Int>()
        val options = GeneratorOptions(length = 8)
        val pools = GeneratorPolicy.pools(options)
        PasswordGenerator.forTest { bound -> bounds += bound; bound - 1 }.generate(options)
        assertEquals(pools.map { it.length }, bounds.take(4))
        assertEquals(List(4) { pools.sumOf(String::length) }, bounds.drop(4).take(4))
        assertEquals((8 downTo 2).toList(), bounds.drop(8))
    }
    @Test fun secureFailurePropagatesWithoutFallback() {
        val generator = PasswordGenerator.forTest { throw java.security.ProviderException("synthetic") }
        assertThrows(java.security.ProviderException::class.java) { generator.generate() }
    }
    @Test fun invalidTestDrawCannotIndexOrWrapSilently() {
        assertThrows(IllegalStateException::class.java) { PasswordGenerator.forTest { it }.generate() }
        assertThrows(IllegalStateException::class.java) { PasswordGenerator.forTest { -1 }.generate() }
    }
}
