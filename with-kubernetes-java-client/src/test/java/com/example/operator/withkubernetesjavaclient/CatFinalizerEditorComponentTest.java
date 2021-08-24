package com.example.operator.withkubernetesjavaclient;

import java.io.FileNotFoundException;
import java.util.UUID;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static com.example.operator.withkubernetesjavaclient.CatFinalizerEditor.FINALIZER_STRING;
import static org.assertj.core.api.Assertions.assertThat;

@WithKubernetesCluster
class CatFinalizerEditorComponentTest {

	private static final String TEST_NAMESPACE = "default";

	@Autowired private OperatorExampleComV1alpha1Api api;
	@Autowired private CatFinalizerEditor finalizerEditor;
	@Autowired private TestK8sClient testK8sClient;

	private String resourceName;

	@BeforeAll
	void createCrd() throws FileNotFoundException, ApiException {
		testK8sClient.createCrd();
	}

	@AfterAll
	void deleteCrd() {
		testK8sClient.deleteCrdIfExists();
	}

	@BeforeEach
	void generateResourceName() {
		resourceName = "cat-" + UUID.randomUUID();
	}

	@AfterEach
	void deleteMapping() {
		testK8sClient.deleteCat(TEST_NAMESPACE, resourceName);
	}

	@Test
	void add_shouldAddFinalizerToCatResource() throws ApiException {
		V1alpha1CatForAdoption existing = testK8sClient.createCat(TEST_NAMESPACE, resourceName);
		V1alpha1CatForAdoption returned = finalizerEditor.add(existing);

		assertThat(returned.getMetadata().getFinalizers()).contains(FINALIZER_STRING);

		V1alpha1CatForAdoption catFromApiServer = api.readNamespacedCatForAdoption(
				resourceName, TEST_NAMESPACE, null, null);
		assertThat(catFromApiServer.getMetadata().getFinalizers()).contains(FINALIZER_STRING);
	}

	@Test
	void remove_shouldRemoveMappingFinalizerToMappingResource() throws ApiException {
		V1ObjectMeta metadata = new V1ObjectMeta().name(resourceName)
												  .addFinalizersItem(FINALIZER_STRING);
		V1alpha1CatForAdoption existing = testK8sClient.createCatWithMetadata(TEST_NAMESPACE, metadata);
		V1alpha1CatForAdoption returned = finalizerEditor.remove(existing);

		assertThat(returned.getMetadata().getFinalizers()).isNull();

		V1alpha1CatForAdoption mappingFromApiServer = api.readNamespacedCatForAdoption(
				resourceName, TEST_NAMESPACE, null, null);
		assertThat(mappingFromApiServer.getMetadata().getFinalizers()).isNull();
	}
}
