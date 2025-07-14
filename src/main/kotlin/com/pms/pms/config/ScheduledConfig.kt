package com.pms.pms.config

import com.pms.pms.repository.OtpRepository
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class ScheduledConfig(
    private val otpRepository: OtpRepository
) {

    // Clean up expired OTPs every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    fun cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps()
    }
} 