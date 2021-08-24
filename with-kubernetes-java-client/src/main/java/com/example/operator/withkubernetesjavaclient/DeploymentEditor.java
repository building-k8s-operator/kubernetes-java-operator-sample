package com.example.operator.withkubernetesjavaclient;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.PatchUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DeploymentEditor {

	private final AppsV1Api appsV1Api;
	private final String adoptionCenterNamespace;
	private final ObjectMapper yamlMapper;
	private final Resource deploymentYaml;

	public DeploymentEditor(
			@Value("classpath:manifests/adoption-center-deployment.yaml") Resource deploymentYaml,
			@Value("${adoption-center.namespace}") String namespace,
			AppsV1Api appsV1Api,
			ObjectMapper yamlMapper) {
		this.deploymentYaml = deploymentYaml;
		this.adoptionCenterNamespace = namespace;
		this.appsV1Api = appsV1Api;
		this.yamlMapper = yamlMapper;
	}

	public V1Deployment createDeployment(V1OwnerReference ownerReference) throws IOException, ApiException {
		V1Deployment body = yamlMapper.readValue(deploymentYaml.getInputStream(), V1Deployment.class);
		body.getMetadata().setName(ownerReference.getName());
		body.getMetadata().setOwnerReferences(Collections.singletonList(ownerReference));
		return appsV1Api.createNamespacedDeployment(adoptionCenterNamespace, body, null, null, null);
	}

	public V1Deployment restartDeployment(String adoptionCenterName) throws ApiException {
		return PatchUtils.patch(
				V1Deployment.class,
				() -> appsV1Api.patchNamespacedDeploymentCall(
						adoptionCenterName,
						adoptionCenterNamespace,
						new V1Patch("\"spec\": {\n" +
								"        \"template\": {\n" +
								"            \"metadata\": {\n" +
								"                \"annotations\": {\n" +
								"                    \"kubectl.kubernetes.io/restartedAt\": " + ZonedDateTime.now(ZoneOffset.UTC) + "\n" +
								"                }\n" +
								"            }\n" +
								"        }\n" +
								"    }"),
						null, null, null, null, null),
				V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH,
				appsV1Api.getApiClient());
		//TODO wait until it's done
	}
}
