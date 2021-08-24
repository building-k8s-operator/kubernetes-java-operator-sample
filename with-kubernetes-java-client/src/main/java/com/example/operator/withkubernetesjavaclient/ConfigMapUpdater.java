package com.example.operator.withkubernetesjavaclient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.example.operator.adoptioncenter.Animal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@Component
public class ConfigMapUpdater {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigMapUpdater.class);

	private final CoreV1Api coreV1Api;
	private final Lister<V1ConfigMap> configMapLister;
	private final String adoptionCenterConfigMapKey;
	private final String adoptionCenterNamespace;
	private final ObjectMapper yamlMapper;

	public ConfigMapUpdater(
			@Value("${adoption-center.configMapKey}") String configMapKey,
			@Value("${adoption-center.namespace}") String namespace,
			Lister<V1ConfigMap> configMapLister,
			CoreV1Api coreV1Api,
			ObjectMapper yamlMapper) {
		this.coreV1Api = coreV1Api;
		this.configMapLister = configMapLister;
		this.adoptionCenterConfigMapKey = configMapKey;
		this.adoptionCenterNamespace = namespace;
		this.yamlMapper = yamlMapper;
	}

	public boolean configMapExists(String adoptionCenterName) {
		LOG.debug("Checking if config map {}/{} exists", adoptionCenterNamespace, adoptionCenterName);
		return (getExistingConfigMap(adoptionCenterName) != null);
	}

	public V1ConfigMap addAnimal(Animal animal, String adoptionCenterName) throws ApiException, JsonProcessingException {
		AnimalsProperties properties = getExistingAnimals(adoptionCenterName);
		properties.getAnimals().add(animal);
		return updateConfigMap(adoptionCenterName, properties);
	}

	public V1ConfigMap updateAnimal(Animal newAnimal, String adoptionCenterName) throws ApiException, JsonProcessingException {
		AnimalsProperties properties = getExistingAnimals(adoptionCenterName);
		Optional<Animal> oldAnimal = properties
				.getAnimals()
				.stream()
				.filter(animal -> isSameAnimal(newAnimal, animal))
				.findFirst();

		if (oldAnimal.isEmpty()) {
			properties.getAnimals().add(newAnimal);
		}
		else {
			oldAnimal.get().setName(newAnimal.getName());
			oldAnimal.get().setDateOfBirth(newAnimal.getDateOfBirth());
			oldAnimal.get().setDescription(newAnimal.getDescription());
		}
		return updateConfigMap(adoptionCenterName, properties);
	}

	public V1ConfigMap removeAnimal(Animal animalToRemove, String adoptionCenterName) throws ApiException, JsonProcessingException {
		AnimalsProperties properties = getExistingAnimals(adoptionCenterName);
		properties.getAnimals().removeIf(animal -> isSameAnimal(animalToRemove, animal));
		return updateConfigMap(adoptionCenterName, properties);
	}

	public V1ConfigMap createConfigMap(V1OwnerReference adoptionCenter) throws ApiException, JsonProcessingException {
		AnimalsProperties properties = new AnimalsProperties(Collections.emptyList());
		V1ConfigMap configMap = new V1ConfigMap()
						.apiVersion("v1")
						.kind("ConfigMap")
						.metadata(new V1ObjectMeta()
								.name(adoptionCenter.getName())
								.namespace(adoptionCenterNamespace)
								.ownerReferences(singletonList(adoptionCenter)));
		addDataToConfigMap(configMap, properties);
		return coreV1Api.createNamespacedConfigMap(
				adoptionCenterNamespace,
				configMap,
				null,
				null,
				null);
	}

	private static boolean isSameAnimal(Animal animalInEvent, Animal animalInConfigMap) {
		return animalInConfigMap.getResourceName().equals(animalInEvent.getResourceName())
				&& animalInConfigMap.getNamespace().equals(animalInEvent.getNamespace());
	}

	private AnimalsProperties getExistingAnimals(String adoptionCenterName) throws JsonProcessingException {
		V1ConfigMap configMap = getExistingConfigMap(adoptionCenterName);
		String serializedAnimals = configMap.getData().get(adoptionCenterConfigMapKey);
		return yamlMapper.readValue(serializedAnimals, ApplicationYaml.class).getAdoptionCenter();
	}

	private V1ConfigMap updateConfigMap(String adoptionCenterName, AnimalsProperties properties) throws JsonProcessingException, ApiException {
		V1ConfigMap configMap = addDataToConfigMap(getExistingConfigMap(adoptionCenterName), properties);
		return coreV1Api.replaceNamespacedConfigMap(
				configMap.getMetadata().getName(),
				adoptionCenterNamespace,
				configMap,
				null,
				null,
				null);
	}

	private V1ConfigMap addDataToConfigMap(V1ConfigMap configMap, AnimalsProperties properties) throws JsonProcessingException {
		String serializedContent = yamlMapper.writeValueAsString(new ApplicationYaml(properties));

		return configMap.data(singletonMap(adoptionCenterConfigMapKey, serializedContent));
	}

	private V1ConfigMap getExistingConfigMap(String adoptionCenterName) {
		return configMapLister.namespace(adoptionCenterNamespace)
		                      .get(adoptionCenterName);
	}

	static class ApplicationYaml {

		private AnimalsProperties adoptionCenter;

		public void setAdoptionCenter(AnimalsProperties adoptionCenter) {
			this.adoptionCenter = adoptionCenter;
		}

		public AnimalsProperties getAdoptionCenter() {
			return adoptionCenter;
		}

		public ApplicationYaml() {
		}

		public ApplicationYaml(AnimalsProperties adoptionCenter) {
			this.adoptionCenter = adoptionCenter;
		}
	}

	static class AnimalsProperties {

		private List<Animal> animals;

		public AnimalsProperties() {
		}

		public AnimalsProperties(List<Animal> animals) {
			this.animals = animals;
		}

		public void setAnimals(List<Animal> animals) {
			this.animals = animals;
		}

		public List<Animal> getAnimals() {
			return animals;
		}
	}
}
