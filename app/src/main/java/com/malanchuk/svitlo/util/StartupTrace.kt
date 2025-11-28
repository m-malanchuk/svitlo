package com.malanchuk.svitlo.util

import android.os.SystemClock
import android.util.Log
import com.malanchuk.svitlo.BuildConfig

object StartupTrace {
    private const val TAG = "SvitloStartup"
    private var processStartMs = SystemClock.elapsedRealtime()

    fun reset() {
        if (BuildConfig.DEBUG) processStartMs = SystemClock.elapsedRealtime()
    }

    fun mark(label: String) {
        if (!BuildConfig.DEBUG) return
        val now = SystemClock.elapsedRealtime()
        Log.i(TAG, "${now}ms (+${now - processStartMs}ms) $label")
    }
}
