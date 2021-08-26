package com.example.operator.withkubernetesjavaclient;

import java.util.MissingResourceException;

import com.example.operator.adoptioncenter.Animal;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.event.EventType;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import static com.example.operator.withkubernetesjavaclient.EventRecorder.toObjectReference;

@Component
public class CatAdoptionReconciler implements Reconciler {

	public static final String CONTROLLER_NAME = "CatAdoptionController";

	private static final Logger LOG = LoggerFactory.getLogger(CatAdoptionReconciler.class);

	private final SharedInformer<V1alpha1CatForAdoption> catInformer;
	private final Lister<V1alpha1CatForAdoption> catLister;
	private final CatFinalizerEditor catFinalizerEditor;
	private final EventRecorder eventRecorder;
	private final ConfigMapUpdater configMapUpdater;
	private final CatStatusEditor catStatusEditor;
	private final DeploymentEditor adoptionCenterRestarter;

	public CatAdoptionReconciler(
			SharedIndexInformer<V1alpha1CatForAdoption> catInformer,
			CatFinalizerEditor catFinalizerEditor,
			EventRecorder eventRecorder,
			ConfigMapUpdater configMapUpdater, CatStatusEditor catStatusEditor, DeploymentEditor deploymentEditor) {
		this.catInformer = catInformer;
		this.catLister = new Lister<>(catInformer.getIndexer());
		this.catFinalizerEditor = catFinalizerEditor;
		this.eventRecorder = eventRecorder;
		this.configMapUpdater = configMapUpdater;
		this.catStatusEditor = catStatusEditor;
		this.adoptionCenterRestarter = deploymentEditor;
	}

	// *OPTIONAL*
	// If you want to hold the controller from running util some condition
	public boolean informerReady() {
		return catInformer.hasSynced();
	}

	public static boolean onDeleteFilter(V1alpha1CatForAdoption catForAdoption, Boolean cacheStatusUnknown) {
		LOG.debug("Going to delete CatForAdoption: {}", catForAdoption.getMetadata().getName());
		return true;
	}

	public static boolean onAddFilter(V1alpha1CatForAdoption catForAdoption) {
		LOG.debug("Received a new CatForAdoption: {}", catForAdoption.getMetadata().getName());
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
				&& cat.getMetadata().getGeneration() > 1
				&& cat.getMetadata().getDeletionTimestamp() == null;

		final boolean toDelete = cat.getMetadata().getDeletionTimestamp() != null;

		String adoptionCenterName = cat.getSpec().getAdoptionCenterName();

		if (!configMapUpdater.configMapExists(adoptionCenterName)) {
			logFailureEvent(cat, "find config map", "The targeted adoption center may not be ready,", new MissingResourceException("ConfigMap not found", V1ConfigMap.class.getName(), V1ConfigMap.class.getName()));
			catStatusEditor.setCatStatus(cat, "Ready", "False", "ConfigMapNotFound");
			return new Result(true);
		}

		Animal animal = catToAnimal(cat);

		try {
			V1ConfigMap updatedConfigMap;
			String reason;
			if (toAdd) {
				addFinalizerIfNotFound(cat);
				catStatusEditor.setCatStatus(cat, "Ready", "false", "AddingCatInConfigMap");
				updatedConfigMap = configMapUpdater.addAnimal(animal, adoptionCenterName);
				reason ="CatAddedToConfigMap";
			} else if (toUpdate) {
				addFinalizerIfNotFound(cat);
				catStatusEditor.setCatStatus(cat, "Ready", "false", "UpdatingCatInConfigMap");
				updatedConfigMap = configMapUpdater.updateAnimal(animal, adoptionCenterName);
				reason = "CatUpdatedInConfigMap";
			} else if (toDelete) {
				catStatusEditor.setCatStatus(cat, "Ready", "false", "RemovingCatFromMConfigMap");
				updatedConfigMap = configMapUpdater.removeAnimal(animal, adoptionCenterName);
				reason = "CatRemovedFromConfigMap";
			} else {
				LOG.error("Illegal state: received a request {} with nothing to do", request);
				throw new RuntimeException("Illegal state: received a request with nothing to do: " + request);
			}

			adoptionCenterRestarter.restartDeployment(adoptionCenterName);

			if (!toDelete) {
				catStatusEditor.setCatStatus(cat, "Ready", "True", reason);
			} else {
				catFinalizerEditor.remove(cat);
			}

			logSuccessEvent(cat, updatedConfigMap, reason);
			return new Result(false);
		}
		catch (ApiException e) {
			e.printStackTrace();
			logFailureEvent(cat, "update adoption-center config map", e.getCode() + " - " + e.getResponseBody(), e);
			return new Result(true);
		}
		catch (JsonProcessingException e) {
			logFailureEvent(cat, "serialize/deserialize yaml in adoption-center config map", e.getMessage(), e);
			return new Result(true);
		}

	}

	private Animal catToAnimal(V1alpha1CatForAdoption cat) {
		Animal animal = new Animal();
		animal.setResourceName(cat.getMetadata().getName());
		animal.setNamespace(cat.getMetadata().getNamespace());
		animal.setName(cat.getSpec().getName());
		animal.setDescription(cat.getSpec().getDescription());
		animal.setDateOfBirth(cat.getSpec().getDateOfBirth());
		return animal;
	}

	private void addFinalizerIfNotFound(V1alpha1CatForAdoption cat) throws ApiException {
		LOG.debug("Checking for existing finalizers");
		boolean notFound = cat.getMetadata().getFinalizers() == null || cat.getMetadata().getFinalizers().isEmpty();
		if (notFound) {
			LOG.debug("Finalizers not found, adding one");
			catFinalizerEditor.add(cat);
		}
	}

	private void logSuccessEvent(V1alpha1CatForAdoption cat, V1ConfigMap updatedConfigMap, String reason) {
		String message = String.format(
				"Successfully updated adoption-center config map with %s/%s",
				cat.getMetadata().getNamespace(), cat.getMetadata().getName());
		eventRecorder.logEvent(toObjectReference(cat), toObjectReference(updatedConfigMap),
							   reason, message, EventType.Normal);
	}

	private void logFailureEvent(V1alpha1CatForAdoption cat, String reason, String errorBody, Exception e) {
		String message = String.format("Failed to %s for cat %s/%s: %s",
				reason, cat.getMetadata().getNamespace(), cat.getMetadata().getName(), errorBody);
		LOG.error(message);
		eventRecorder.logEvent(
				toObjectReference(cat),
				null,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}
}
