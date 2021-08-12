package com.example.operator.withkubernetesjavaclient;

import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import io.kubernetes.client.openapi.models.V1Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class CatAdoptionReconciler implements Reconciler {

	public static final String CONTROLLER_NAME = "CatAdoptionController";

	private static final Logger LOG = LoggerFactory.getLogger(CatAdoptionReconciler.class);

	private final SharedInformer<V1alpha1CatForAdoption> catInformer;

	private final Lister<V1alpha1CatForAdoption> catLister;

	private final CatFinalizerEditor catFinalizerEditor;

	private final EventRecorder eventRecorder;

	public CatAdoptionReconciler(
			SharedIndexInformer<V1alpha1CatForAdoption> catInformer,
			CatFinalizerEditor catFinalizerEditor,
			EventRecorder eventRecorder) {
		this.catInformer = catInformer;
		this.catLister = new Lister<>(catInformer.getIndexer());
		this.catFinalizerEditor = catFinalizerEditor;
		this.eventRecorder = eventRecorder;
	}

	// *OPTIONAL*
	// If you want to hold the controller from running util some condition..
	public boolean informerReady() {
		return catInformer.hasSynced();
	}

	public static boolean onDeleteFilter(V1alpha1CatForAdoption catForAdoption, Boolean cacheStatusUnknown) {
		LOG.debug("Going to delete CatForAdoption: {}", catForAdoption);
		return false;
	}

	public static boolean onAddFilter(V1alpha1CatForAdoption catForAdoption) {
		LOG.debug("Received a new CatForAdoption: {}", catForAdoption);
		return true;
	}

	public static boolean onUpdateFilter(V1alpha1CatForAdoption oldCat, V1alpha1CatForAdoption newCat) {
		return !oldCat.getMetadata().getGeneration().equals(newCat.getMetadata().getGeneration());
	}

	@Override
	public Result reconcile(Request request) {
		V1alpha1CatForAdoption cat = catLister.namespace(request.getNamespace()).get(request.getName());
		LOG.debug("Expected state {}", cat);

		final boolean toAdd = cat.getMetadata().getGeneration() == null
				|| cat.getMetadata().getGeneration() == 1;

		final boolean toUpdate = cat.getMetadata().getGeneration() != null
				&& cat.getMetadata().getGeneration() > 1;

		final boolean toDelete = cat.getMetadata().getDeletionTimestamp() != null;


//		RoutesDefinition routesDefinition = routesDefinitionResolver.getRoutes(cat);
//		for (V1Pod pod : gatewayPods) {
//			try {
//				String hostHeader = ActuatorRoutesUpdater.buildHostHeader(request, pod);
//				if (toDelete) {
//					this.actuatorRoutesUpdater.deleteMapping(pod, routesDefinition, hostHeader);
//					logSuccessEvent(cat, pod, "Deleted");
//				}
//				else if (toUpdate) {
//					this.actuatorRoutesUpdater.updateMapping(pod, routesDefinition, hostHeader);
//					logSuccessEvent(cat, pod, "Updated");
//				}
//				else if (toAdd) {
//					this.actuatorRoutesUpdater.addMapping(pod, routesDefinition, hostHeader);
//					logSuccessEvent(cat, pod, "Created");
//				}
//				else {
//					LOG.error("Illegal state: received a request {} with nothing to do", request);
//				}
//			}
//			catch (PodUpdateException e) {
//				logFailureEvent(cat, pod, e);
//				return new Result(true);
//			}
//		}

		try {
			if (toDelete) {
				catFinalizerEditor.remove(cat);
			}
			else if (finalizerNotFound(cat)) {
				catFinalizerEditor.add(cat);
			}
		}
		catch (ApiException e) {
			logFailureEvent(cat, e);
			return new Result(true);
		}

		return new Result(false);
	}

	private boolean finalizerNotFound(V1alpha1CatForAdoption cat) {
		return cat.getMetadata().getFinalizers() == null || cat.getMetadata().getFinalizers().isEmpty();
	}

	private void logFailureEvent(V1alpha1CatForAdoption cat, ApiException e) {
		String message = String.format("Failed to edit finalizer for cat %s/%s",
				cat.getMetadata().getNamespace(),
				cat.getMetadata().getName());
		LOG.error(message);
		eventRecorder.logEvent(
				toObjectReference(cat),
				new V1ObjectReference()
						.namespace(cat.getMetadata().getNamespace())
						.name("Finalizer"),
				"ApiException",
				message + ": " + e.getMessage(),
				EventType.Warning);
	}

	public static V1ObjectReference toObjectReference(KubernetesObject k8sObject) {
		return new V1ObjectReference()
				.apiVersion(k8sObject.getApiVersion())
				.kind(k8sObject.getKind())
				.uid(k8sObject.getMetadata().getUid())
				.name(k8sObject.getMetadata().getName())
				.namespace(k8sObject.getMetadata().getNamespace());
	}

}
