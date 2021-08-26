package com.example.operator.withkubernetesjavaclient;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.example.operator.adoptioncenter.Animal;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@WithKubernetesCluster
@Tag("acceptance")
class FeatureTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTest.class);

	@Value("${adoption-center.namespace}")
	private String adoptionCenterNamespace;

	@Autowired
	private TestK8sClient testK8sClient;

	private WebTestClient webTestClient = WebTestClient.bindToServer().responseTimeout(Duration.ofMinutes(20)).build();

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
	void journeyTest() throws Exception {
		V1alpha1AdoptionCenter adoptionCenter = testK8sClient.createAdoptionCenter();
		assertThatSubResourcesAreDeployed(adoptionCenter);
		assertThatAdoptionCenterHasCat(adoptionCenter.getMetadata().getName(), 0);

		V1alpha1CatForAdoption cat = testK8sClient.createCat("default", "my-cat");
		waitForReconciliationtoTriggerRestart();
		assertThatAdoptionCenterHasCat(adoptionCenter.getMetadata().getName(), 1, cat);

		testK8sClient.deleteCat("default", "my-cat");
		waitForReconciliationtoTriggerRestart();
		assertThatAdoptionCenterHasCat(adoptionCenter.getMetadata().getName(), 0);

		testK8sClient.deleteAdoptionCenter(adoptionCenter.getMetadata().getName());
		assertThatSubResourcesAreDeleted(adoptionCenter);
	}

	private void waitForReconciliationtoTriggerRestart() {
		await().timeout(Duration.ofMinutes(20)).pollDelay(Duration.ofSeconds(2)).until(() -> true);
	}

	private void assertThatAdoptionCenterHasCat(String adoptionCenterName, int expectedCatCount, V1alpha1CatForAdoption... addedCats) throws Exception {
		testK8sClient.waitForDeploymentReady(adoptionCenterNamespace, adoptionCenterName);
		testK8sClient.forwardDeployment(adoptionCenterNamespace, adoptionCenterName, 8080, uriBuilder -> {
			LOGGER.info("Pulling animals endpoint");
			List<Animal> animals = webTestClient
					.get()
					.uri(uriBuilder
							.path("animals")
							.build())
					.exchange()
					.expectStatus().is2xxSuccessful()
					.expectBodyList(Animal.class)
					.returnResult()
					.getResponseBody();

			assertThat(animals).hasSize(expectedCatCount);
			for (V1alpha1CatForAdoption cat : addedCats) {
				assertThat(animals).anyMatch(animal ->
						animal.getName().equals(cat.getSpec().getName()) &&
								animal.getNamespace().equals(cat.getMetadata().getNamespace()) &&
								animal.getResourceName().equals(cat.getMetadata().getName()) &&
								animal.getDescription().equals(cat.getSpec().getDescription()) &&
								animal.getDateOfBirth().equals(cat.getSpec().getDateOfBirth()));
			}
		});
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
