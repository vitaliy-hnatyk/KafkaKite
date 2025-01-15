package kafkakite.controller

import kafkakit.Application
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
class ApplicationControllerTest (
    @Autowired val mockMvc: MockMvc
) {

    @Test
    fun `test GET root endpoint`() {
        mockMvc.get("/")
            .andExpect {
                status { isOk() }
                content { string("Kafka Kite") }
            }
    }
}