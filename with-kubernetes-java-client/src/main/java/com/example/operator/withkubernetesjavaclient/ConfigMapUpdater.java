package com.example.operator.withkubernetesjavaclient;

import java.util.List;
import java.util.Optional;

import com.example.operator.adoptioncenter.Animal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Value;

import static java.util.Collections.singletonMap;

public class ConfigMapUpdater {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigMapUpdater.class);
	private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	private static final Yaml yaml = new Yaml();

	private final CoreV1Api coreV1Api;
	private final Lister<V1ConfigMap> configMapLister;
	private final String adoptionCenterConfigMapName;
	private final String adoptionCenterConfigMapKey;
	private final String adoptionCenterNamespace;

	public ConfigMapUpdater(
			@Value("${adoption-center.configMapName}") String configMapName,
			@Value("${adoption-center.configMapKey}") String configMapKey,
			@Value("${adoption-center.namespace}") String namespace,
			Lister<V1ConfigMap> configMapLister,
			CoreV1Api coreV1Api) {
		this.coreV1Api = coreV1Api;
		this.configMapLister = configMapLister;
		this.adoptionCenterConfigMapName = configMapName;
		this.adoptionCenterConfigMapKey = configMapKey;
		this.adoptionCenterNamespace = namespace;
	}

	public boolean configMapExists() {
		LOG.debug("Getting configmap {}/{}", adoptionCenterNamespace, adoptionCenterConfigMapName);
		V1ConfigMap configMap = configMapLister.namespace(adoptionCenterNamespace)
		                                       .get(adoptionCenterConfigMapName);
		return (configMap != null);
	}

	public V1ConfigMap addAnimal(Animal animal) throws ApiException {
		AnimalsProperties properties = getExistingAnimals();
		properties.getAnimals().add(animal);
		return updateConfigMap(properties);
	}

	public V1ConfigMap updateAnimal(Animal newAnimal) throws ApiException {
		AnimalsProperties properties = getExistingAnimals();
		Optional<Animal> oldAnimal = properties
				.getAnimals()
				.stream()
				.filter(animal -> isSameAnimal(newAnimal, animal))
				.findFirst();

		if (oldAnimal.isEmpty()) {
			properties.getAnimals().add(newAnimal);
		}
		else {
			oldAnimal.get().setDateOfBirth(newAnimal.getDateOfBirth());
			oldAnimal.get().setDescription(newAnimal.getDescription());
		}
		return updateConfigMap(properties);
	}

	public V1ConfigMap removeAnimal(Animal animalToRemove) throws ApiException {
		AnimalsProperties properties = getExistingAnimals();
		properties.getAnimals().removeIf(animal -> isSameAnimal(animalToRemove, animal));
		return updateConfigMap(properties);
	}

	private boolean isSameAnimal(Animal animalInEvent, Animal animalInConfigMap) {
		return animalInConfigMap.getResourceName().equals(animalInEvent.getResourceName())
				&& animalInConfigMap.getNamespace().equals(animalInEvent.getNamespace());
	}

	private AnimalsProperties getExistingAnimals() {
		V1ConfigMap configMap = configMapLister.namespace(adoptionCenterNamespace)
		                                       .get(adoptionCenterConfigMapName);
		String serializedAnimals = configMap.getData().get(adoptionCenterConfigMapKey);
		try {
			return mapper.readValue(serializedAnimals, ApplicationYaml.class).getAdoptionCenter();
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
			return null; //TODO
		}
	}

	private V1ConfigMap updateConfigMap(AnimalsProperties properties) throws ApiException {
//		final String serializedContent = yaml.dumpAs(new ApplicationYaml(properties), Tag.MAP, null);
		String serializedContent = "";
		try {
			serializedContent = mapper.writeValueAsString(new ApplicationYaml(properties));
		}
		catch (JsonProcessingException e) {
			e.printStackTrace(); // TODO
		}

		V1ConfigMap configMap = new V1ConfigMap()
				.apiVersion("v1")
				.kind("ConfigMap")
				.metadata(new V1ObjectMeta()
						.name(adoptionCenterConfigMapName)
						.namespace(adoptionCenterNamespace)) // TODO: add owner reference
				.data(singletonMap(adoptionCenterConfigMapKey, serializedContent));
		return coreV1Api.replaceNamespacedConfigMap(
				configMap.getMetadata().getName(),
				adoptionCenterNamespace,
				configMap,
				null,
				null,
				null);
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
