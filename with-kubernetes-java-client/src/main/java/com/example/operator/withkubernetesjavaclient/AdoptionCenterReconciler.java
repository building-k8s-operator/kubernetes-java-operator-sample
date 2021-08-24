package com.example.operator.withkubernetesjavaclient;

import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.cache.Lister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class AdoptionCenterReconciler implements Reconciler {

	public static final String CONTROLLER_NAME = "CatAdoptionController";

	private static final Logger LOG = LoggerFactory.getLogger(AdoptionCenterReconciler.class);

	private final Lister<V1alpha1AdoptionCenter> adoptionCenterLister;
	private final EventRecorder eventRecorder;

	public AdoptionCenterReconciler(
			Lister<V1alpha1AdoptionCenter> adoptionCenterLister,
			EventRecorder eventRecorder) {
		this.adoptionCenterLister = adoptionCenterLister;
		this.eventRecorder = eventRecorder;
	}

	@Override
	public Result reconcile(Request request) {
		LOG.debug("Received request {}", request);
		V1alpha1AdoptionCenter adoptionCenter = adoptionCenterLister.get(request.getName());


		return new Result(false);
	}

}
