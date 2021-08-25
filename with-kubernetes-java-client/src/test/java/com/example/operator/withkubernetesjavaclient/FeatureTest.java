package com.example.operator.withkubernetesjavaclient;

import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@WithKubernetesCluster
@Tag("acceptance")
class FeatureTest {

	@Value("${adoption-center.namespace}")
	private String adoptionCenterNamespace;

	@Autowired
	private TestK8sClient testK8sClient;

	@BeforeAll
	void createResources() {
		testK8sClient.createAdoptionCenterCrd();
		testK8sClient.createCatCrd();
		testK8sClient.createNamespace(adoptionCenterNamespace);
	}

	@AfterAll
	void deleteResources() {
		testK8sClient.deleteCatCrdIfExists();
		testK8sClient.deleteAdoptionCenterCrdIfExists();
		testK8sClient.deleteNamespace(adoptionCenterNamespace);
	}

	@Test
	void journeyTest() {
		V1alpha1AdoptionCenter adoptionCenter = testK8sClient.createAdoptionCenter();
		assertThatSubResourcesAreDeployed(adoptionCenter);
		//TODO: call /animals endpoint from the adoption center, verify 200
		//TODO: add a cat resource, and see endpoint update with new cat after cat status turns ready
		testK8sClient.deleteAdoptionCenter(adoptionCenter.getMetadata().getName());
		assertThatSubResourcesAreDeleted(adoptionCenter);
	}

	private void assertThatSubResourcesAreDeleted(V1alpha1AdoptionCenter adoptionCenter) {
		await()
				.atMost(Duration.ofMinutes(2))
				.untilAsserted(() -> testK8sClient.getDeployment(adoptionCenterNamespace, adoptionCenter.getMetadata().getName())
				                                  .isEmpty());
		await()
				.atMost(Duration.ofMinutes(2))
				.untilAsserted(() -> testK8sClient.getConfigMap(adoptionCenterNamespace, adoptionCenter.getMetadata().getName())
				                                  .isEmpty());
	}

	private void assertThatSubResourcesAreDeployed(V1alpha1AdoptionCenter adoptionCenter) {
		V1Deployment deployment = await()
				.atMost(Duration.ofMinutes(2))
				.until(
						() -> testK8sClient.getDeployment(adoptionCenterNamespace, adoptionCenter.getMetadata().getName()),
						Optional::isPresent)
				.get();
		V1ConfigMap configMap = await()
				.atMost(Duration.ofMinutes(2))
				.until(
						() -> testK8sClient.getConfigMap(adoptionCenterNamespace, adoptionCenter.getMetadata().getName()),
						Optional::isPresent)
				.get();
		assertThat(deployment.getMetadata().getOwnerReferences().get(0).getKind()).isEqualTo("AdoptionCenter");
		assertThat(configMap.getMetadata().getOwnerReferences().get(0).getKind()).isEqualTo("AdoptionCenter");
	}

}
