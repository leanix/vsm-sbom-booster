package net.leanix.vsm.sbomBooster.configuration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.annotation.PostConstruct
import net.leanix.vsm.sbomBooster.service.OrtService

open class GenericTestConfiguration {
    @MockkBean(relaxed = true)
    protected lateinit var ortService: OrtService

    @MockkBean
    protected lateinit var propertiesConfiguration: PropertiesConfiguration

    @PostConstruct
    fun setUp() {
        every { propertiesConfiguration.concurrencyFactor } returns 2
        every { propertiesConfiguration.mountedVolume } returns "mountedVolume"
        every { propertiesConfiguration.allowNoComponentSboms } returns false
        every { propertiesConfiguration.devMode } returns false
        every { propertiesConfiguration.ortImage } returns "leanixacrpublic.azurecr.io/ort"
        every { propertiesConfiguration.leanIxHost } returns "de"
        every { propertiesConfiguration.leanIxToken } returns "dummyLeanIxToken"
        every { propertiesConfiguration.sourceInstance } returns "sourceInstance"
        every { propertiesConfiguration.sourceType } returns "sourceType"
    }
}
