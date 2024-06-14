package com.example.RetailService;

import org.springframework.boot.SpringApplication;

public class TestRetailServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RetailServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
