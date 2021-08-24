package com.example.operator.withkubernetesjavaclient;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.example.operator.withkubernetesjavaclient.apis.OperatorExampleComV1alpha1Api;
import com.example.operator.withkubernetesjavaclient.models.V1alpha1CatForAdoption;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.PatchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class CatStatusEditor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CatStatusEditor.class);

	private final OperatorExampleComV1alpha1Api api;

	public CatStatusEditor(
			OperatorExampleComV1alpha1Api api) {this.api = api;}

	public void setCatStatus(V1alpha1CatForAdoption cat, String type, String status, String reason) {
		String patch = String.format("{\"status\": " +
											 "{ \"conditions\": " +
											 "[{ \"type\": \"%s\", \"status\": \"%s\", \"lastTransitionTime\": \"%s\", \"reason\": \"%s\"}]" +
											 "}}",
									 type,
									 status,
									 ZonedDateTime.now(ZoneOffset.UTC),
									 reason);
		try {
			PatchUtils.patch(
					V1alpha1CatForAdoption.class,
					() -> api
							.patchNamespacedCatForAdoptionStatusCall(
									cat.getMetadata().getName(),
									cat.getMetadata().getNamespace(),
									new V1Patch(patch),
									null,
									null,
									null,
									null),
					V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH,
					api.getApiClient());

		}
		catch (ApiException e) {
			LOGGER.error("Status API call failed: {}: {}, {}, with patch {}",
						 e.getCode(),
						 e.getMessage(),
						 e.getResponseBody(),
						 patch);
		}
	}

}
