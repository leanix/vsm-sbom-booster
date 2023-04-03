package net.leanix.vsm.sbomBooster.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableAsync
@EnableScheduling
class AsyncConfiguration(
    private val propertiesConfiguration: PropertiesConfiguration
) {

    @Bean(name = ["taskExecutor"])
    fun threadPoolTaskExecutor(): ThreadPoolTaskExecutor? {
        val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
        threadPoolTaskExecutor.corePoolSize = propertiesConfiguration.concurrencyFactor

        return threadPoolTaskExecutor
    }

    @Bean(name = ["threadPoolTaskScheduler"])
    fun threadPoolTaskScheduler(): ThreadPoolTaskScheduler? {
        val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
        threadPoolTaskScheduler.poolSize = 1

        return threadPoolTaskScheduler
    }
}
