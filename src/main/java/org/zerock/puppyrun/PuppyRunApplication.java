package org.zerock.puppyrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PuppyRunApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuppyRunApplication.class, args);
    }

}
