package com.vis.bob.iso8583;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class Iso8583BridgeApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(Iso8583BridgeApplication.class, args);

		log.info("ISO-8583 Bridge Server -   Running");

	}

}
