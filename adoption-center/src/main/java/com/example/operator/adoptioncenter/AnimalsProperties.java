package com.example.operator.adoptioncenter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("adoption-center")
@ConstructorBinding
public class AnimalsProperties {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimalController.class);

	private final List<Animal> animals;

	public AnimalsProperties(List<Animal> animals) {
		LOGGER.info("Loading {} animals", animals.size());
		this.animals = animals;
	}

	public List<Animal> getAnimals() {
		return animals;
	}
}
