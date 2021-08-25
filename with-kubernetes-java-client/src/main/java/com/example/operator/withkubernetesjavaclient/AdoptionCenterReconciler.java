package com.example.operator.withkubernetesjavaclient;

import com.example.operator.withkubernetesjavaclient.models.V1alpha1AdoptionCenter;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.example.operator.withkubernetesjavaclient.EventRecorder.toObjectReference;

@Component
public class AdoptionCenterReconciler implements Reconciler {

	public static final String CONTROLLER_NAME = "AdoptionCenterController";

	private static final Logger LOG = LoggerFactory.getLogger(AdoptionCenterReconciler.class);

	private final Lister<V1alpha1AdoptionCenter> adoptionCenterLister;
	private final ConfigMapUpdater configMapUpdater;
	private final DeploymentEditor deploymentEditor;
	private final EventRecorder eventRecorder;
	private final String adoptionCenterNamespace;

	public AdoptionCenterReconciler(
			@Value("${adoption-center.namespace}") String namespace,
			Lister<V1alpha1AdoptionCenter> adoptionCenterLister,
			ConfigMapUpdater configMapUpdater, DeploymentEditor deploymentEditor, EventRecorder eventRecorder) {
		this.adoptionCenterLister = adoptionCenterLister;
		this.configMapUpdater = configMapUpdater;
		this.deploymentEditor = deploymentEditor;
		this.eventRecorder = eventRecorder;
		this.adoptionCenterNamespace = namespace;
	}

	@Override
	public Result reconcile(Request request) {
		LOG.debug("Received request {}", request);
		V1alpha1AdoptionCenter adoptionCenter = adoptionCenterLister.get(request.getName());
		LOG.debug("Received adoption center resource {}", adoptionCenter);

		try {
			V1OwnerReference ownerReference = toOwnerReference(adoptionCenter);
			if (!configMapUpdater.configMapExists(ownerReference.getName())) {
				configMapUpdater.createConfigMap(ownerReference);
			}
			deploymentEditor.createDeployment(ownerReference);
		}
		catch (ApiException e) {
			if (e.getCode() == 409) {
				LOG.info("Required subresource is already present, skip creation.");
				return new Result(false);
			}
			logFailureEvent(adoptionCenter, e.getCode() + " - " + e.getResponseBody(), e);
			return new Result(true);
		}
		catch (Exception e) {
			logFailureEvent(adoptionCenter, e.getMessage(), e);
			return new Result(true);
		}
		return new Result(false);
	}

	private V1OwnerReference toOwnerReference(V1alpha1AdoptionCenter adoptionCenter) {
		return new V1OwnerReference().controller(true)
		                             .name(adoptionCenter.getMetadata().getName())
		                             .uid(adoptionCenter.getMetadata().getUid())
		                             .kind(adoptionCenter.getKind())
		                             .apiVersion(adoptionCenter.getApiVersion())
		                             .blockOwnerDeletion(true);
	}

	private void logFailureEvent(V1alpha1AdoptionCenter adoptionCenter, String reason, Exception e) {
		String message = String.format("Failed to deploy adoption center %s: %s", adoptionCenter.getMetadata().getName(), reason);
		LOG.error(message);
		eventRecorder.logEvent(
				toObjectReference(adoptionCenter).namespace(adoptionCenterNamespace),
				null,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}
}
