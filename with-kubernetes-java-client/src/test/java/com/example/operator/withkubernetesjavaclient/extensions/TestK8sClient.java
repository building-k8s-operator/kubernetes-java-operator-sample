package com.example.operator.withkubernetesjavaclient.extensions;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class TestK8sClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestK8sClient.class);
	private static final String CAT_CRD_PATH = "../crds/cat-custom-resource-definition.yaml";
	private static final String ADOPTION_CENTER_CRD_PATH = "../crds/adoption-center-custom-resource-definition.yaml";

	private final ApiextensionsV1Api apiextensionsV1Api;
	private final AppsV1Api appsV1Api;
	private final CoreV1Api coreV1Api;
	private final OperatorExampleComV1alpha1Api operatorExampleComV1alpha1Api;
	private final V1CustomResourceDefinition catCrd;
	private final V1CustomResourceDefinition adoptionCenterCrd;
	private final ObjectMapper yaml;

	public TestK8sClient(ApiClient apiClient) throws IOException {
		appsV1Api = new AppsV1Api(apiClient);
		apiextensionsV1Api = new ApiextensionsV1Api(apiClient);
		coreV1Api = new CoreV1Api(apiClient);
		operatorExampleComV1alpha1Api = new OperatorExampleComV1alpha1Api(apiClient);
		yaml = new ObjectMapper(new YAMLFactory());
		yaml.registerModule(new JavaTimeModule());
		catCrd = yaml.readValue(new FileInputStream(CAT_CRD_PATH), V1CustomResourceDefinition.class);
		adoptionCenterCrd = yaml.readValue(new FileInputStream(ADOPTION_CENTER_CRD_PATH), V1CustomResourceDefinition.class);
	}

	public Optional<V1Deployment> getDeployment(String namespace, String name) {
		try {
			V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(
					namespace, null, null, null, null, null, null, null, null, null, null);

			return deploymentList
					.getItems().stream()
					.filter(v1Deployment -> name.equals(v1Deployment.getMetadata().getName()))
					.findFirst();
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public void deleteDeploymentIfExists(String deploymentName) {
		try {
			appsV1Api.deleteNamespacedDeployment(deploymentName, "default", null, null, null, null,
					null, null);
		}
		catch (ApiException e) {
			if (e.getCode() == 404) {
				LOGGER.warn("Deployment not found, skipping");
				return;
			}

			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}

	}

	public V1alpha1CatForAdoption createCatWithMetadata(String namespace, V1ObjectMeta metadata) {
		return createCat(mapping -> mapping.metadata(metadata.namespace(namespace)));
	}

	public V1alpha1CatForAdoption createCat(Consumer<V1alpha1CatForAdoption> customizer) {
		try {
			V1alpha1CatForAdoption cat = yaml.readValue(new FileInputStream("../manifests/test-cat.yaml"), V1alpha1CatForAdoption.class);
			customizer.accept(cat);

			return operatorExampleComV1alpha1Api.createNamespacedCatForAdoption(
					cat.getMetadata().getNamespace(), cat, null, null, null);
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public V1alpha1CatForAdoption createCat(String namespace, String name) {
		return createCat(TestK8sClient.<V1alpha1CatForAdoption>nsCustomizer(namespace).andThen(nameCustomizer(name)));
	}

	public void deleteCat(String namespace, String resourceName) {
		try {
			operatorExampleComV1alpha1Api
					.deleteNamespacedCatForAdoption(resourceName, namespace, null, null, null, null, null, null);
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public V1alpha1AdoptionCenter createAdoptionCenter() {
		try {
			V1alpha1AdoptionCenter definition = yaml.readValue(new FileInputStream("../manifests/test-adoption-center.yaml"), V1alpha1AdoptionCenter.class);
			return operatorExampleComV1alpha1Api.createAdoptionCenter(definition, null, null, null);
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void deleteAdoptionCenter(String name) {
		try {
			operatorExampleComV1alpha1Api.deleteAdoptionCenter(name, null, null, null, null, null, null);
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public String createCatCrd() {
		return createCrd(CAT_CRD_PATH);
	}

	public void deleteCatCrdIfExists() {
		deleteCrdIfExists(catCrd.getMetadata().getName());
	}

	public String createAdoptionCenterCrd() {
		return createCrd(ADOPTION_CENTER_CRD_PATH);
	}

	public void deleteAdoptionCenterCrdIfExists() {
		deleteCrdIfExists(adoptionCenterCrd.getMetadata().getName());
	}

	public String createCrd(String filePath) {
		V1CustomResourceDefinition definition = null;
		try {
			definition = yaml.readValue(new FileInputStream(filePath), V1CustomResourceDefinition.class);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			apiextensionsV1Api.createCustomResourceDefinition(definition, null, null, null);
			return definition.getMetadata().getName();
		}
		catch (ApiException e) {
			if (e.getCode() == 409) {
				LOGGER.warn("CRD already exists, skipping");
				return definition.getMetadata().getName();
			}
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public void deleteCrdIfExists(String crdName) {
		try {
			apiextensionsV1Api.deleteCustomResourceDefinition(crdName, null, null, null, null, null, null);
		}
		catch (ApiException e) {
			if (e.getCode() == 404) {
				LOGGER.warn("CRD not found, skipping");
				return;
			}

			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public V1ConfigMap createConfigMap(String namespace, String name) {
		V1ConfigMap body = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(namespace));
		try {
			return coreV1Api.createNamespacedConfigMap(namespace, body, null, null, null);
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public Optional<V1ConfigMap> getConfigMap(String namespace, String name) {
		try {
			V1ConfigMapList configMapList = coreV1Api.listNamespacedConfigMap(
					namespace, null, null, null, null, null, null, null, null, null, null);

			return configMapList
					.getItems().stream()
					.filter(it -> name.equals(it.getMetadata().getName()))
					.findFirst();
		}
		catch (ApiException e) {
			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public void deleteConfigMapIfExists(String namespace, String configMap) {
		try {
			coreV1Api.deleteNamespacedConfigMap(configMap, namespace, null, null, null, null, null, null);
		}
		catch (ApiException e) {
			if (e.getCode() == 404) {
				LOGGER.warn("ConfigMap not found, skipping");
				return;
			}

			LOGGER.error("Delete API request failed: {}: {}, {}", e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public void createNamespace(String namespaceName) {
		V1Namespace namespace = new V1Namespace()
				.apiVersion("v1")
				.kind("Namespace")
				.metadata(new V1ObjectMeta().name(namespaceName));

		try {
			coreV1Api.createNamespace(namespace, null, null, null);
			LOGGER.info("Create namespace {}: SUCCESS", namespaceName);
		}
		catch (ApiException e) {
			LOGGER.error("Create namespace {}: FAILED with {}-{}, {}",
					namespaceName, e.getCode(), e.getMessage(), e.getResponseBody());
		}
	}

	public void deleteNamespace(String namespaceName) {
		try {
			coreV1Api.deleteNamespace(
					namespaceName,
					null,
					null,
					null,
					null,
					null,
					null);
			LOGGER.info("Delete namespace {}: SUCCESS", namespaceName);
		}
		catch (ApiException e) {
			LOGGER.error("Create namespace {}: FAILED with {}-{}, {}",
					namespaceName, e.getCode(), e.getMessage(), e.getResponseBody());
			throw new RuntimeException(e);
		}
	}

	public static <T extends KubernetesObject> Consumer<T> nsCustomizer(String ns) {
		return k8sObj -> k8sObj.getMetadata().namespace(ns);
	}

	public static <T extends KubernetesObject> Consumer<T> nameCustomizer(String name) {
		return k8sObj -> k8sObj.getMetadata().name(name);
	}
}
