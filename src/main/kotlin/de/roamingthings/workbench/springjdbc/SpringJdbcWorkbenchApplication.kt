package de.roamingthings.workbench.springjdbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringJdbcWorkbenchApplication

fun main(args: Array<String>) {
    runApplication<SpringJdbcWorkbenchApplication>(*args)
}
