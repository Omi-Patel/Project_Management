package com.pms.pms

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ProjectManagementSystemApplication

fun main(args: Array<String>) {
    runApplication<ProjectManagementSystemApplication>(*args)
}
