package com.example.operator.withkubernetesjavaclient;

import com.example.operator.adoptioncenter.Animal;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static com.example.operator.withkubernetesjavaclient.PurrfectConfiguration.yamlMapper;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigMapUpdaterTest {

	private static final String TEST_CONFIG_MAP_NAME = "test-config-map-name";
	private static final String TEST_CONFIG_MAP_KEY = "test-config-map-key";
	private static final String TEST_NAMESPACE = "test-namespace";

	@Mock
	private CoreV1Api coreV1Api;
	@Mock
	private Lister<V1ConfigMap> configMapLister;
	@Captor
	private ArgumentCaptor<V1ConfigMap> configMapArgumentCaptor;

	private ConfigMapUpdater configMapUpdater;

	@BeforeEach
	void setUp() {
		configMapUpdater = new ConfigMapUpdater(TEST_CONFIG_MAP_KEY, TEST_NAMESPACE, configMapLister, coreV1Api, yamlMapper());
	}

	@Test
	void configMapExists() {
		mockGatewayLister(defaultConfigMap());
		assertThat(configMapUpdater.configMapExists(TEST_CONFIG_MAP_NAME)).isTrue();

		mockGatewayLister(null);
		assertThat(configMapUpdater.configMapExists(TEST_CONFIG_MAP_NAME)).isFalse();
	}

	@Test
	void createConfigMap() throws ApiException, JsonProcessingException {
		V1OwnerReference ownerReference = new V1OwnerReference().name(TEST_CONFIG_MAP_NAME);
		configMapUpdater.createConfigMap(ownerReference);

		verify(coreV1Api).createNamespacedConfigMap(eq(TEST_NAMESPACE),
				configMapArgumentCaptor.capture(), isNull(), isNull(), isNull());

		assertThat(configMapArgumentCaptor.getValue().getMetadata().getName()).isEqualTo(TEST_CONFIG_MAP_NAME);
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getOwnerReferences()).isEqualTo(singletonList(ownerReference));
		assertThat(configMapArgumentCaptor.getValue().getData()).containsKey(TEST_CONFIG_MAP_KEY);
		assertThat(configMapArgumentCaptor.getValue().getData().get(TEST_CONFIG_MAP_KEY))
				.isEqualTo("---\n" +
						"adoptionCenter:\n" +
						"  animals: []\n");
	}

	@Test
	void addAnimal() throws ApiException, JsonProcessingException {
		mockGatewayLister(defaultConfigMap());
		mockConfigMapUpdateCall();

		configMapUpdater.addAnimal(getAnimal("chocobo", "test-cat-name", "boston"), TEST_CONFIG_MAP_NAME);

		verifyConfigMapUpdateCall();
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getName()).isEqualTo(TEST_CONFIG_MAP_NAME);
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(configMapArgumentCaptor.getValue().getData()).containsKey(TEST_CONFIG_MAP_KEY);
		assertThat(configMapArgumentCaptor.getValue().getData().get(TEST_CONFIG_MAP_KEY))
				.isEqualTo("---\n" +
						"adoptionCenter:\n" +
						"  animals:\n" +
						"  - name: \"test-cat-name\"\n" +
						"    resourceName: \"chocobo\"\n" +
						"    namespace: \"boston\"\n" +
						"    dateOfBirth: \"" + LocalDate.now() + "\"\n" +
						"    description: \"test-cat-description\"\n");
	}

	@Test
	void updateAnimal() throws ApiException, JsonProcessingException {
		mockGatewayLister(configMapWithExistingAnimal("paprika", "test-cat-name", "boston"));
		mockConfigMapUpdateCall();

		configMapUpdater.updateAnimal(getAnimal("paprika", "chocobo", "boston"), TEST_CONFIG_MAP_NAME);

		verifyConfigMapUpdateCall();
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getName()).isEqualTo(TEST_CONFIG_MAP_NAME);
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(configMapArgumentCaptor.getValue().getData()).containsKey(TEST_CONFIG_MAP_KEY);
		assertThat(configMapArgumentCaptor.getValue().getData().get(TEST_CONFIG_MAP_KEY))
				.isEqualTo("---\n" +
						"adoptionCenter:\n" +
						"  animals:\n" +
						"  - name: \"chocobo\"\n" +
						"    resourceName: \"paprika\"\n" +
						"    namespace: \"boston\"\n" +
						"    dateOfBirth: \"" + LocalDate.now() + "\"\n" +
						"    description: \"test-cat-description\"\n");
	}

	@Test
	void removeAnimal() throws JsonProcessingException, ApiException {
		String resourceName = "chocobo";
		mockGatewayLister(configMapWithExistingAnimal(resourceName, "test-cat-name", "boston"));
		mockConfigMapUpdateCall();

		configMapUpdater.removeAnimal(getAnimal(resourceName, "test-cat-name", "boston"), TEST_CONFIG_MAP_NAME);

		verifyConfigMapUpdateCall();
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getName()).isEqualTo(TEST_CONFIG_MAP_NAME);
		assertThat(configMapArgumentCaptor.getValue().getMetadata().getNamespace()).isEqualTo(TEST_NAMESPACE);
		assertThat(configMapArgumentCaptor.getValue().getData()).containsKey(TEST_CONFIG_MAP_KEY);
		assertThat(configMapArgumentCaptor.getValue().getData().get(TEST_CONFIG_MAP_KEY))
				.isEqualTo("---\n" +
						"adoptionCenter:\n" +
						"  animals: []\n");
	}

	private void mockConfigMapUpdateCall() throws ApiException {
		when(coreV1Api.replaceNamespacedConfigMap(anyString(), anyString(), any(V1ConfigMap.class), isNull(), isNull(), isNull()))
				.thenReturn(defaultConfigMap());
	}

	private void mockGatewayLister(V1ConfigMap configMap) {
		when(configMapLister.namespace(TEST_NAMESPACE)).thenReturn(mock(Lister.class));
		when(configMapLister.namespace(TEST_NAMESPACE).get(TEST_CONFIG_MAP_NAME)).thenReturn(configMap);
	}

	private void verifyConfigMapUpdateCall() throws ApiException {
		verify(coreV1Api).replaceNamespacedConfigMap(eq(TEST_CONFIG_MAP_NAME), eq(TEST_NAMESPACE),
				configMapArgumentCaptor.capture(), isNull(), isNull(), isNull());
	}

	private Animal getAnimal(String resourceName, String catName, String namespace) {
		Animal animal = new Animal();
		animal.setResourceName(resourceName);
		animal.setNamespace(namespace);
		animal.setName(catName);
		animal.setDescription("test-cat-description");
		animal.setDateOfBirth(LocalDate.now());
		return animal;
	}

	private V1ConfigMap defaultConfigMap() {
		return configMap("---\n" +
				"adoptionCenter:\n" +
				"  animals: []");
	}

	private V1ConfigMap configMapWithExistingAnimal(String resourceName, String catName, String namespace) {
		return configMap("---\n" +
				"adoptionCenter:\n" +
				"  animals:\n" +
				"  - name: \"" + catName + "\"\n" +
				"    resourceName: \"" + resourceName + "\"\n" +
				"    namespace: \"" + namespace + "\"\n" +
				"    dateOfBirth: \"" + LocalDate.now() + "\"\n" +
				"    description: \"test-cat-description\"\n");
	}

	private V1ConfigMap configMap(String yamlContent) {
		return new V1ConfigMap()
				.apiVersion("v1")
				.kind("ConfigMap")
				.metadata(new V1ObjectMeta()
						.name(TEST_CONFIG_MAP_NAME)
						.namespace(TEST_NAMESPACE))
				.data(Collections.singletonMap(TEST_CONFIG_MAP_KEY, yamlContent));
	}
}
