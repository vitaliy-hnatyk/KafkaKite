package kafkakit


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication(scanBasePackages = ["kafkakite.controller"])
class Application

fun main(args: Array<String>) {
        runApplication<Application>(*args)
}


