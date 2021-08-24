package com.example.operator.withkubernetesjavaclient;

import java.time.Duration;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenterList;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoptionList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.DefaultControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ConfigMapList;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PurrfectConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(PurrfectConfiguration.class);

	@Bean(destroyMethod = "shutdown")
	ControllerManager controllerManager(SharedInformerFactory sharedInformerFactory, Controller[] controllers) {
		return new ControllerManager(sharedInformerFactory, controllers);
	}

	@Bean
	CommandLineRunner commandLineRunner(ControllerManager controllerManager) {
		return args -> new Thread(controllerManager, "ControllerManager").start();
	}

	@Bean
	public Controller catController(
			SharedInformerFactory sharedInformerFactory,
			CatAdoptionReconciler reconciler) {
		DefaultControllerBuilder builder = ControllerBuilder.defaultBuilder(sharedInformerFactory);
		builder = builder.watch(
				(q) -> ControllerBuilder.controllerWatchBuilder(V1alpha1CatForAdoption.class, q)
				                        .withOnDeleteFilter(CatAdoptionReconciler::onDeleteFilter)
				                        .withOnUpdateFilter(CatAdoptionReconciler::onUpdateFilter)
				                        .withOnAddFilter(CatAdoptionReconciler::onAddFilter)
				                        .withResyncPeriod(Duration.ofMinutes(1))
				                        .build());
		builder.withWorkerCount(2);
		builder.withReadyFunc(reconciler::informerReady);
		return builder.withReconciler(reconciler)
		              .withName(CatAdoptionReconciler.CONTROLLER_NAME)
		              .build();
	}

	@Bean
	public Controller adoptionCenterController(
			SharedInformerFactory sharedInformerFactory,
			AdoptionCenterReconciler reconciler) {
		DefaultControllerBuilder builder = ControllerBuilder.defaultBuilder(sharedInformerFactory);
		builder = builder.watch(
				(q) -> ControllerBuilder.controllerWatchBuilder(V1alpha1AdoptionCenter.class, q)
				                        .withOnDeleteFilter((resource, cache) -> false)
				                        .withOnUpdateFilter((oldOne, newOne) -> false)
				                        .withOnAddFilter((resource) -> true)
				                        .withResyncPeriod(Duration.ofMinutes(1))
				                        .build());
		builder.withWorkerCount(2);
		return builder.withReconciler(reconciler)
		              .withName(AdoptionCenterReconciler.CONTROLLER_NAME)
		              .build();
	}

	@Bean
	public Lister<V1alpha1AdoptionCenter> adoptionCenterInformer(
			ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1alpha1AdoptionCenter, V1alpha1AdoptionCenterList> genericApi =
				new GenericKubernetesApi<>(
						V1alpha1AdoptionCenter.class,
						V1alpha1AdoptionCenterList.class,
						"operator.example.com",
						"v1alpha1",
						"adoptioncenters",
						apiClient);
		SharedIndexInformer<V1alpha1AdoptionCenter> informer = sharedInformerFactory
				.sharedIndexInformerFor(genericApi, V1alpha1AdoptionCenter.class, 0);
		return new Lister<>(informer.getIndexer());
	}

	@Bean
	public SharedIndexInformer<V1alpha1CatForAdoption> catInformer(
			ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1alpha1CatForAdoption, V1alpha1CatForAdoptionList> genericApi =
				new GenericKubernetesApi<>(
						V1alpha1CatForAdoption.class,
						V1alpha1CatForAdoptionList.class,
						"operator.example.com",
						"v1alpha1",
						"catsforadoption",
						apiClient);
		return sharedInformerFactory.sharedIndexInformerFor(genericApi, V1alpha1CatForAdoption.class, 0);
	}

	@Bean
	public Lister<V1ConfigMap> configMapLister(
			@Value("${adoption-center.namespace}") String adoptionCenterNamespace,
			ApiClient apiClient,
			SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1ConfigMap, V1ConfigMapList> genericApi =
				new GenericKubernetesApi<>(V1ConfigMap.class, V1ConfigMapList.class, "", "v1", "configmaps", apiClient);
		SharedIndexInformer<V1ConfigMap> informer = sharedInformerFactory.sharedIndexInformerFor(genericApi, V1ConfigMap.class, 60 * 1000L, adoptionCenterNamespace);
		return new Lister<>(informer.getIndexer());
	}

	@Bean
	public ConfigMapUpdater configMapUpdater(ApiClient apiClient,
	                                         @Value("${adoption-center.configMapName}") String configMapName,
	                                         @Value("${adoption-center.configMapKey}") String configMapKey,
	                                         @Value("${adoption-center.namespace}") String namespace,
	                                         Lister<V1ConfigMap> configMapLister) {
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);

		return new ConfigMapUpdater(configMapName, configMapKey, namespace, configMapLister, coreV1Api);
	}

	@Bean
	EventRecorder eventClient(ApiClient client) {
		return new EventRecorder(new EventsV1Api(client));
	}

	@Bean
	OperatorExampleComV1alpha1Api operatorExampleComV1alpha1Api(ApiClient client) {
		return new OperatorExampleComV1alpha1Api(client);
	}

}
