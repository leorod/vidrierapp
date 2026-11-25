package com.vidrierapp.server.service

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MaintenanceWorker(private val repository: VidrierAppRepository) {
    private val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "vidrierapp-maintenance").apply { isDaemon = true }
    }

    fun start() {
        executor.scheduleAtFixedRate(
            { runCatching { repository.expireStaleShops(); repository.expireAccessStatuses(); repository.refreshAggregates() } },
            1,
            60,
            TimeUnit.MINUTES,
        )
    }
}
