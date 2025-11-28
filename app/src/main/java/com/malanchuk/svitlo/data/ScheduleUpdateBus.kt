package com.malanchuk.svitlo.data

import com.malanchuk.svitlo.model.ScheduleSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-local notification for an already persisted schedule update.
 *
 * The snapshot on disk remains the durable source of truth. This bus only
 * lets an already visible screen render a background update immediately.
 */
object ScheduleUpdateBus {
    private val _updates = MutableSharedFlow<ScheduleSnapshot>(extraBufferCapacity = 1)

    val updates = _updates.asSharedFlow()

    fun publish(snapshot: ScheduleSnapshot) {
        _updates.tryEmit(snapshot)
    }
}
