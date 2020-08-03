package wd

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WDCompoundTest {
    lateinit var wdSparql: WDSparql

    @BeforeEach
    fun setUp() {
        wdSparql = WDSparql()
    }

    @Test
    fun findCompoundByInChIKey() {
        val wdCompound = WDCompound(wdSparql)
        val inChIKey = "VFLDPWHFBUODDF-FCXRPNKRSA-N"
        val expected = "Q312266"
        assertArrayEquals(arrayOf(expected), wdCompound.findCompoundByInChIKey(inChIKey)[inChIKey]?.toTypedArray())
    }

    @Test
    fun findCompoundsByInChIKey() {
        val wdCompound = WDCompound(wdSparql)
        val inChIs = mapOf(
            "VFLDPWHFBUODDF-FCXRPNKRSA-N" to listOf("Q312266"),
            "REFJWTPEDVJJIY-UHFFFAOYSA-N" to listOf("Q409478")
        )
        val result = wdCompound.findCompoundsByInChIKey(inChIs.keys.toList())
        result.forEach {
            assertEquals(inChIs[it.key], it.value)
        }
    }

    @Test
    fun findCompoundsByInChIKeyChunked() {
        val wdCompound = WDCompound(wdSparql)
        val inChIs = mapOf(
            "VFLDPWHFBUODDF-FCXRPNKRSA-N" to listOf("Q312266"),
            "REFJWTPEDVJJIY-UHFFFAOYSA-N" to listOf("Q409478"),
            "DMULVCHRPCFFGV-UHFFFAOYSA-N" to listOf("Q407217"),
            "RYYVLZVUVIJVGH-UHFFFAOYSA-N" to listOf("Q60235")

        )
        val result = wdCompound.findCompoundsByInChIKey(inChIs.keys.toList(), chunkSize = 2)
        result.forEach {
            assertEquals(inChIs[it.key], it.value)
        }
    }
}