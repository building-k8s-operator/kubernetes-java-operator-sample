package com.example.operator.adoptioncenter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class AnimalControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getAllAnimals() {
		webTestClient
			.get()
			.uri("/animals")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.length()").isEqualTo(4)
			.jsonPath("$[0].name").isEqualTo("Chocobo")
			.jsonPath("$[0].resourceName").isEqualTo("chocobo")
			.jsonPath("$[0].namespace").isEqualTo("boston")
			.jsonPath("$[0].dateOfBirth").isEqualTo("2014-09-25");
	}
}
