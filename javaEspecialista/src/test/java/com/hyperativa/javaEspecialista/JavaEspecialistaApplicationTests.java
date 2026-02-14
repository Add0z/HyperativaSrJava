package com.hyperativa.javaEspecialista;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JavaEspecialistaApplicationTests {

	@Test
	void contextLoads() {
	}

}
