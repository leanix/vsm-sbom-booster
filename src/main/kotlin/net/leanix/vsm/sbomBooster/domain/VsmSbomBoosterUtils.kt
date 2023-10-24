package net.leanix.vsm.sbomBooster.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

object VsmSbomBoosterUtils {
    private val logger: Logger = LoggerFactory.getLogger(VsmSbomBoosterUtils::class.java)

    fun deleteFolder(folder: String?) {
        if (folder != null) {
            logger.info("Beginning to delete folder $folder.")
            val absoluteFolder = Paths.get("tempDir", folder).toFile()
            absoluteFolder.deleteRecursively()
            logger.info("Finished deleting temp folder $folder.")
        }
    }

    fun writeStringToFile(parentFolderPath: String, fileName: String, content: String) {
        val directory = File(parentFolderPath)

        // Create the directory if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)

        // Write the string to the file
        file.writeText(content)
    }
}
