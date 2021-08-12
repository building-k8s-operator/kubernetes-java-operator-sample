package com.example.operator.withkubernetesjavaclient;

import java.time.Duration;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoptionList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.DefaultControllerBuilder;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PurrfectConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(PurrfectConfiguration.class);

	@Bean
	public CommandLineRunner commandLineRunner(
			SharedInformerFactory sharedInformerFactory, Controller catController) {
		return args -> {
			LOG.info("starting informers..");
			sharedInformerFactory.startAllRegisteredInformers();

			LOG.info("running controller..");
			catController.run();
		};
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
	EventRecorder eventClient(ApiClient client) {
		return new EventRecorder(new EventsV1Api(client));
	}

	@Bean
	OperatorExampleComV1alpha1Api operatorExampleComV1alpha1Api(ApiClient client) {
		return new OperatorExampleComV1alpha1Api(client);
	}

}
