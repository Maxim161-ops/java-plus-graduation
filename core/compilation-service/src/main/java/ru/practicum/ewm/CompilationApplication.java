package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "ru.practicum.ewm",
        "ru.practicum.stats.client"
})
@EnableFeignClients(basePackages = "ru.practicum.interaction.client")
public class CompilationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompilationApplication.class, args);
    }
}