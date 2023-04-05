package net.leanix.vsm.sbomBooster.service

import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.time.Instant

@Service
class SummaryReportService {
    private val summaryFile = Paths.get("tempDir", "summaryReport_${Instant.now()}.txt").toFile()

    fun appendRecord(record: String) {
        summaryFile.appendText(record)
    }
}
