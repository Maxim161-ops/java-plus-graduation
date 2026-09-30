package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients(basePackages = "ru.practicum.interaction.client")
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.ewm",
        "ru.practicum.stats.client"
})
public class RequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RequestApplication.class, args);
    }
}
