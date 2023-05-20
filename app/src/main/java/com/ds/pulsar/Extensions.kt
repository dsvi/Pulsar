package com.ds.pulsar

import android.content.res.Resources
import androidx.compose.ui.unit.Dp
import java.util.*


val String.uuid: UUID get() = UUID.fromString(this)

val Int.spToDp: Dp get() = Dp(this.toFloat() * Resources.getSystem().displayMetrics.scaledDensity)

