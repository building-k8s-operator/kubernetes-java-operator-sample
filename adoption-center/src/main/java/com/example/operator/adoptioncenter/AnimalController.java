package com.example.operator.adoptioncenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableConfigurationProperties(AnimalsProperties.class)
public class AnimalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimalController.class);

	private final AnimalsProperties animalsProperties;

	public AnimalController(AnimalsProperties animalsProperties) {
		this.animalsProperties = animalsProperties;
	}

	@GetMapping("/animals")
	public Iterable<Animal> getAllAnimals() {
		LOGGER.info("Received get all animals request");
		return animalsProperties.getAnimals();
	}
}
