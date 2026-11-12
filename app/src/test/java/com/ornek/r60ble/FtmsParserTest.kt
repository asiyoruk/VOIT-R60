package com.ornek.r60ble

import com.ornek.r60ble.data.ble.FtmsParser
import org.junit.Assert.*
import org.junit.Test

class FtmsParserTest {
    @Test fun parsesSpeedCadencePower() {
        // flags: bit2 (cadence) + bit6 (power) = 0x44 ; hız alanı var (bit0 = 0)
        val data = byteArrayOf(0x44, 0x00, 0xC4.toByte(), 0x09, 0xB4.toByte(), 0x00, 0x96.toByte(), 0x00)
        val m = FtmsParser.parse(data, 1000L, null)!!
        assertEquals(25.0, m.speed, 0.01)    // 2500 * 0.01
        assertEquals(90.0, m.cadence, 0.01)  // 180 * 0.5
        assertEquals(150, m.power)
    }
    @Test fun truncatedPacketReturnsNull() {
        assertNull(FtmsParser.parse(byteArrayOf(0x44, 0x00, 0x01), 0L, null))
    }
}
