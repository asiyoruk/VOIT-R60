package com.ornek.r60ble.util

import java.text.SimpleDateFormat
import java.util.*

fun formatDuration(sec: Long): String = "%02d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
fun formatDate(t: Long): String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(t))
