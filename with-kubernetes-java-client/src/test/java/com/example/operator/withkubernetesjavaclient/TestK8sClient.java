package com.example.operator.withkubernetesjavaclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonSyntaxException;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.ApiextensionsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1CustomResourceDefinition;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class TestK8sClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestK8sClient.class);
	private static final String CAT_CRD_PATH = "../crds/cat-custom-resource-definition.yaml";

	private final ApiextensionsV1Api apiextensionsV1Api;
	private final AppsV1Api appsV1Api;
	private final CoreV1Api coreV1Api;
	private final OperatorExampleComV1alpha1Api operatorExampleComV1alpha1Api;
	private final V1CustomResourceDefinition crd;
	private final ObjectMapper yaml;

	public TestK8sClient(ApiClient apiClient) throws IOException {
		appsV1Api = new AppsV1Api(apiClient);
		apiextensionsV1Api = new ApiextensionsV1Api(apiClient);
		coreV1Api = new CoreV1Api(apiClient);
		operatorExampleComV1alpha1Api = new OperatorExampleComV1alpha1Api(apiClient);
		yaml = new ObjectMapper(new YAMLFactory());
		yaml.registerModule(new JavaTimeModule());
		crd = yaml.readValue(new FileInputStream(CAT_CRD_PATH), V1CustomResourceDefinition.class);
	}

	public boolean deploymentExists(String deploymentName) {
		return getDeployment(deploymentName).isPresent();
	}

	public Optional<V1Deployment> getDeployment(String deploymentName) {
		try {
			V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(
					"default", null, null, null, null, null, null, null, null, null, null);

			Optional<V1Deployment> deployment = deploymentList
					.getItems().stream()
					.filter(v1Deployment -> deploymentName.equals(v1Deployment.getMetadata().getName()))
					.findFirst();

			return deployment;
		}
		catch (ApiException e) {
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
			throw new RuntimeException(e);
		}
	}

	public String createCrd() throws IOException, ApiException {
		V1CustomResourceDefinition definition;
		definition = yaml.readValue(
				new FileInputStream(CAT_CRD_PATH),
				V1CustomResourceDefinition.class
		);

		apiextensionsV1Api.createCustomResourceDefinition(definition, null, null, null);

		return definition.getMetadata().getName();
	}

	public V1ConfigMap createConfigMap(String namespace, String name) throws ApiException {
		V1ConfigMap body = new V1ConfigMap().metadata(new V1ObjectMeta().name(name).namespace(namespace));
		return coreV1Api.createNamespacedConfigMap(namespace, body, null, null, null);
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

			throw new RuntimeException(e);
		}
	}

	public void deleteCrdIfExists() {
		try {
			apiextensionsV1Api.deleteCustomResourceDefinition(crd.getMetadata().getName(), null, null, null, null, null, null);
		}
		catch (ApiException e) {
			if (e.getCode() == 404) {
				LOGGER.warn("CRD not found, skipping");
				return;
			}

			throw new RuntimeException(e);
		}
		catch (JsonSyntaxException e) {
			// some weird response from the API, but CRD is actually deleted
			LOGGER.warn("Failed to parse response from API but deletion succeeded: {}", e.getMessage());
		}
	}

	public static <T extends KubernetesObject> Consumer<T> nsCustomizer(String ns) {
		return k8sObj -> k8sObj.getMetadata().namespace(ns);
	}

	public static <T extends KubernetesObject> Consumer<T> nameCustomizer(String name) {
		return k8sObj -> k8sObj.getMetadata().name(name);
	}
}
