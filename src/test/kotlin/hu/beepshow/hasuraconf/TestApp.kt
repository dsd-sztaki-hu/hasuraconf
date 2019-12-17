package hu.beepshow.hasuraconf

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// A dummy to bootstrap all the spring boot mechanisms for testing
@SpringBootApplication
class TestApp

fun main(args: Array<String>) {
    runApplication<TestApp>(*args)
}
