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

	public CatAdoptionReconciler(
			SharedIndexInformer<V1alpha1CatForAdoption> catInformer,
			CatFinalizerEditor catFinalizerEditor,
			EventRecorder eventRecorder,
			ConfigMapUpdater configMapUpdater, CatStatusEditor catStatusEditor) {
		this.catInformer = catInformer;
		this.catLister = new Lister<>(catInformer.getIndexer());
		this.catFinalizerEditor = catFinalizerEditor;
		this.eventRecorder = eventRecorder;
		this.configMapUpdater = configMapUpdater;
		this.catStatusEditor = catStatusEditor;
	}

	// *OPTIONAL*
	// If you want to hold the controller from running util some condition
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
		LOG.trace("Expected state {}", cat);

		final boolean toAdd = cat.getMetadata().getGeneration() == null
				|| cat.getMetadata().getGeneration() == 1;

		final boolean toUpdate = cat.getMetadata().getGeneration() != null
				&& cat.getMetadata().getGeneration() > 1;

		final boolean toDelete = cat.getMetadata().getDeletionTimestamp() != null;

		if (!configMapUpdater.configMapExists()) {
			logFailureEvent(cat, "find config map", new MissingResourceException("ConfigMap not found", V1ConfigMap.class.getName(), V1ConfigMap.class.getName()));
			catStatusEditor.setCatStatus(cat, "Ready", "False", "ConfigMapNotFound");
			return new Result(true);
		}

		Animal animal = catToAnimal(cat);
		try {
			if (toAdd) {
				LOG.debug("Adding animal {} to configmap", animal);
				V1ConfigMap updatedConfigMap = configMapUpdater.addAnimal(animal);
				catStatusEditor.setCatStatus(cat, "Ready", "True", "CatAddedToConfigMap");
				logSuccessEvent(cat, updatedConfigMap, "Added");
			} else if (toUpdate) {
				LOG.debug("Updating animal {} in configmap", animal);
				catStatusEditor.setCatStatus(cat, "Ready", "false", "UpdatingCatInConfigMap");
				V1ConfigMap updatedConfigMap = configMapUpdater.updateAnimal(animal);
				catStatusEditor.setCatStatus(cat, "Ready", "True", "CatUpdatedInConfigMap");
				logSuccessEvent(cat, updatedConfigMap,"Updated");
			} else if (toDelete) {
				LOG.debug("Removing animal {} from configmap", animal);
				catStatusEditor.setCatStatus(cat, "Ready", "false", "RemovingCatFromMConfigMap");
				V1ConfigMap updatedConfigMap = configMapUpdater.removeAnimal(animal);
				logSuccessEvent(cat, updatedConfigMap, "Removed");
			} else {
				LOG.error("Illegal state: received a request {} with nothing to do", request);
			}
		}
		catch (ApiException e) {
			logFailureEvent(cat, "update adoption-center config map", e);
			return new Result(true);
		}
		catch (JsonProcessingException e) {
			logFailureEvent(cat, "serialize/deserialize yaml in adoption-center config map", e);
			return new Result(true);
		}

		try {
			if (toDelete) {
				catFinalizerEditor.remove(cat);
			}
			else if (finalizerNotFound(cat)) {
				catFinalizerEditor.add(cat);
			}
		}
		catch (ApiException e) {
			logFailureEvent(cat, "edit finalizer", e);
			return new Result(true);
		}

		return new Result(false);
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

	private boolean finalizerNotFound(V1alpha1CatForAdoption cat) {
		return cat.getMetadata().getFinalizers() == null || cat.getMetadata().getFinalizers().isEmpty();
	}

	private void logSuccessEvent(V1alpha1CatForAdoption cat, V1ConfigMap updatedConfigMap, String reason) {
		String message = String.format(
				"Successfully updated adoption-center config map with %s/%s",
				cat.getMetadata().getNamespace(), cat.getMetadata().getName());
		eventRecorder.logEvent(toObjectReference(cat), toObjectReference(updatedConfigMap),
							   reason, message, EventType.Normal);
	}

	private void logFailureEvent(V1alpha1CatForAdoption cat, String reason, Exception e) {
		String message = String.format("Failed to %s for cat %s/%s",
				reason,
				cat.getMetadata().getNamespace(),
				cat.getMetadata().getName());
		LOG.error(message);
		eventRecorder.logEvent(
				toObjectReference(cat),
				null,
				e.getClass().getName(),
				message + ": " + e.getMessage(),
				EventType.Warning);
	}
}
