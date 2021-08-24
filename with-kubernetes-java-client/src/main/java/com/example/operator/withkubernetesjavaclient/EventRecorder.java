package com.example.operator.withkubernetesjavaclient;

import java.time.OffsetDateTime;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.EventsV1Api;
import io.kubernetes.client.openapi.models.EventsV1Event;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.lang.Nullable;

public class EventRecorder {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventRecorder.class);
	private static final String ACTION = "Reconcile";

	private final EventsV1Api eventsApi;

	public EventRecorder(EventsV1Api eventsApi) {this.eventsApi = eventsApi;}

	public void logEvent(V1ObjectReference regardingObjectRef, @Nullable V1ObjectReference relatedObjectRef,
						 String reason, String message, EventType type) {
		try {
			eventsApi.createNamespacedEvent(
					regardingObjectRef.getNamespace(),
					new EventsV1Event()
							.metadata(new V1ObjectMeta()
									.generateName(regardingObjectRef.getName() + "-")
									.namespace(regardingObjectRef.getNamespace()))
							.type(type.toString())
							.action(ACTION)
							.reason(reason)
							.note(message)
							.reportingInstance(CatAdoptionReconciler.CONTROLLER_NAME)
							.regarding(regardingObjectRef)
							.related(relatedObjectRef)
							.eventTime(OffsetDateTime.now())
							.reportingController(CatAdoptionReconciler.CONTROLLER_NAME),
					null,
					null,
					null);
		}
		catch (ApiException e) {
			if (e.getCode() == 404) {
				LOGGER.error("Unable to create events using events.k8s.io/v1 API");
				return;
			}
			if (e.getResponseBody() != null) {
				message += ". Event creation failed due to: " + e.getResponseBody();
			}
			LOGGER.error(message + e.getResponseBody(), e);
		}
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
