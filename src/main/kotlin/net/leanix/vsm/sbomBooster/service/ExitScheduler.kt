package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.VsmSbomBoosterApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.*
import kotlin.system.exitProcess

@Component
class ExitScheduler(
    @Qualifier("taskExecutor") private val threadPoolTaskExecutor: ThreadPoolTaskExecutor
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExitScheduler::class.java)
    }

    @Scheduled(fixedRate = 15000, initialDelay = 30000)
    fun checkPendingTasks() {
        val tasksSubmitted = threadPoolTaskExecutor.threadPoolExecutor.taskCount
        val tasksCompleted = threadPoolTaskExecutor.threadPoolExecutor.completedTaskCount
        val pendingTasks = tasksSubmitted - tasksCompleted

        logger.info(
            "Progress  ${String.format(
                Locale.ENGLISH,
                "%.2f", (tasksCompleted.toDouble() / tasksSubmitted.toDouble()) * 100
            )} %" +
                " - Tasks submitted: $tasksSubmitted, Tasks completed: $tasksCompleted, Tasks pending: $pendingTasks"
        )
        if (pendingTasks == 0L) {
            logger.info("Submitted ${VsmSbomBoosterApplication.counter} services with SBOM file to VSM.")
            exitProcess(1)
        }
    }
}
