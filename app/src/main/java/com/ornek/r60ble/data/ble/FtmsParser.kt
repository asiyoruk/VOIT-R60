package com.ornek.r60ble.data.ble

import com.ornek.r60ble.domain.LiveMetric

/** FTMS "Indoor Bike Data" (0x2AD2) ayrıştırıcı. */
object FtmsParser {
    fun parse(data: ByteArray, now: Long, prev: LiveMetric?): LiveMetric? = try {
        var i = 0
        fun u8() = data[i++].toInt() and 0xFF
        fun u16(): Int { val lo = u8(); val hi = u8(); return lo or (hi shl 8) }
        val flags = u16()
        fun has(bit: Int) = (flags and (1 shl bit)) != 0

        var speed = prev?.speed ?: 0.0
        var cadence = prev?.cadence ?: 0.0
        var power = prev?.power ?: 0
        var distance: Double? = null

        if (!has(0)) speed = u16() * 0.01          // km/h
        if (has(1)) u16()                          // ort. hız
        if (has(2)) cadence = u16() * 0.5          // rpm
        if (has(3)) u16()                          // ort. kadans
        if (has(4)) { val a = u8(); val b = u8(); val c = u8(); distance = (a or (b shl 8) or (c shl 16)).toDouble() }
        if (has(5)) u16()                          // direnç
        if (has(6)) power = u16().toShort().toInt()
        // enerji, nabız vb. alanlar şimdilik okunmuyor

        val dist = distance ?: run {
            val dt = if (prev != null) (now - prev.timestamp) / 1000.0 else 0.0
            (prev?.distance ?: 0.0) + speed / 3.6 * dt
        }
        LiveMetric(now, speed, cadence, power, dist)
    } catch (e: IndexOutOfBoundsException) {
        null
    }
}
