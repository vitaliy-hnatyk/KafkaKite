package kafkakite.controller


import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ApplicationController {

    @GetMapping("/")
    fun index(): String {
        return "Kafka Kite"
    }
}
