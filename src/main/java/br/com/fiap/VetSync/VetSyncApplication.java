package br.com.fiap.VetSync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class VetSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(VetSyncApplication.class, args);
    }

}