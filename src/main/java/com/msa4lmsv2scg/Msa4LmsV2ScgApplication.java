package com.msa4lmsv2scg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Msa4LmsV2ScgApplication {

    public static void main(String[] args) {
        SpringApplication.run(Msa4LmsV2ScgApplication.class, args);
    }

}
