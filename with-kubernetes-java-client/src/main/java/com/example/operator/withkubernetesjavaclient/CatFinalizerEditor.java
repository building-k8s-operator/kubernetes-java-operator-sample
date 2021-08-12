package com.example.operator.withkubernetesjavaclient;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.PatchUtils;

import org.springframework.stereotype.Component;

@Component
public class CatFinalizerEditor {

	private final OperatorExampleComV1alpha1Api api;
	static final String FINALIZER_STRING = "operator.example.com/finalizer";

	public CatFinalizerEditor(OperatorExampleComV1alpha1Api api) {this.api = api;}

	public V1alpha1CatForAdoption add(V1alpha1CatForAdoption mappingToPatch) throws ApiException {
		return PatchUtils.patch(
				V1alpha1CatForAdoption.class,
				() -> api.patchNamespacedCatForAdoptionCall(
						mappingToPatch.getMetadata().getName(),
						mappingToPatch.getMetadata().getNamespace(),
						new V1Patch("{\"metadata\":{\"finalizers\":[\"" + FINALIZER_STRING + "\"]}}"),
						null, null, null, null),
				V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
				api.getApiClient());
	}

	public V1alpha1CatForAdoption remove(V1alpha1CatForAdoption mappingToPatch) throws ApiException {
		return PatchUtils.patch(
				V1alpha1CatForAdoption.class,
				() -> api.patchNamespacedCatForAdoptionCall(
						mappingToPatch.getMetadata().getName(),
						mappingToPatch.getMetadata().getNamespace(),
						new V1Patch("[{\"op\": \"remove\", \"path\": \"/metadata/finalizers\"}]"), // Currently we don't have other finalizers so for now we just recklessly remove all finalizers.
						null, null, null, null),
				V1Patch.PATCH_FORMAT_JSON_PATCH,
				api.getApiClient());
	}
}
