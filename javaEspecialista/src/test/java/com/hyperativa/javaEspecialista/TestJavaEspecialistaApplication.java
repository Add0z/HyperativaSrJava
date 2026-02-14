package com.hyperativa.javaEspecialista;

import org.springframework.boot.SpringApplication;


public class TestJavaEspecialistaApplication {

	public static void main(String[] args) {
		SpringApplication.from(JavaEspecialistaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
