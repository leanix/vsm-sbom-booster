package net.leanix.vsm.sbomBooster.service

import net.leanix.vsm.sbomBooster.VsmSbomBoosterApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*
import kotlin.system.exitProcess

@Component
class ExitScheduler(
    @Qualifier("taskExecutor") private val threadPoolTaskExecutor: ThreadPoolTaskExecutor,
    private val summaryReportService: SummaryReportService
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ExitScheduler::class.java)
    }

    @Scheduled(fixedRate = 100000, initialDelay = 30000)
    fun checkPendingTasks() {
        val tasksSubmitted = threadPoolTaskExecutor.threadPoolExecutor.taskCount
        val tasksCompleted = threadPoolTaskExecutor.threadPoolExecutor.completedTaskCount
        val tasksActive = threadPoolTaskExecutor.threadPoolExecutor.activeCount
        val pendingTasks = tasksSubmitted - tasksCompleted

        logger.info(
            "Progress: ${String.format(
                Locale.ENGLISH,
                "%.2f", getPercentage(tasksCompleted.toDouble(), tasksSubmitted.toDouble())
            )} % " +
                "Success rate: ${String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    getPercentage(VsmSbomBoosterApplication.counter.get().toDouble(), tasksCompleted.toDouble())
                )} % " +
                " - Tasks submitted: $tasksSubmitted, Tasks completed: $tasksCompleted," +
                " Tasks pending: $pendingTasks, Tasks active: $tasksActive"
        )
        if (VsmSbomBoosterApplication.gotRepositories && pendingTasks == 0L) {
            logger.info("Submitted ${VsmSbomBoosterApplication.counter.get()} services with SBOM file to VSM.")
            logger.info("You can find a detailed summary report inside the mounted folder.")
            summaryReportService.appendRecord(
                "\nSubmitted ${VsmSbomBoosterApplication.counter.get()} " +
                    "services with SBOM file to VSM."
            )
            summaryReportService.appendRecord("\nTotal repositories discovered were $tasksSubmitted.")
            summaryReportService.appendRecord(
                "\nTotal success rate was ${String.format(
                    Locale.ENGLISH,
                    "%.2f",
                    getPercentage(VsmSbomBoosterApplication.counter.get().toDouble(), tasksSubmitted.toDouble())
                )} %."
            )

            summaryReportService.appendRecord("\nFinished VSM SBOM Booster at ${LocalDateTime.now()} \n")
            exitProcess(1)
        }
    }

    private fun getPercentage(numerator: Double, denominator: Double): Double {
        if (denominator == 0.0) return denominator
        return (numerator / denominator) * 100
    }
}
