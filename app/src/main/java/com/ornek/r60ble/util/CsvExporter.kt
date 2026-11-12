package com.ornek.r60ble.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ornek.r60ble.domain.WorkoutSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    private fun fmt(t: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(t))

    fun toCsv(s: WorkoutSession): String = buildString {
        appendLine("startTime,endTime,durationSec,distanceKm,totalCalories,avgPower,maxPower")
        appendLine("${fmt(s.startTime)},${fmt(s.endTime)},${s.durationSec}," +
            "%.3f,%.1f,%.1f,%d".format(Locale.US, s.distanceKm, s.totalCalories, s.avgPower, s.maxPower))
        appendLine()
        appendLine("timestamp,speedKmh,cadenceRpm,powerW,distanceM")
        s.metrics.forEach {
            appendLine("${fmt(it.timestamp)},%.2f,%.1f,%d,%.1f".format(Locale.US, it.speed, it.cadence, it.power, it.distance))
        }
    }

    /** Önbelleğe yazar; paylaşılabilir Uri döner (FileProvider). */
    fun export(context: Context, s: WorkoutSession): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "session_${s.startTime}.csv").apply { writeText(toCsv(s)) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
