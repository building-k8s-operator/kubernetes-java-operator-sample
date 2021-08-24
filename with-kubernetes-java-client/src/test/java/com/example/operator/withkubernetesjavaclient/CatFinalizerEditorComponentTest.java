//package com.example.operator.withkubernetesjavaclient;
//
//import java.io.FileNotFoundException;
//import java.util.UUID;
//
//import com.vmware.tanzu.springcloudgateway.KubernetesComponentTest;
//import com.vmware.tanzu.springcloudgateway.TestK8SClient;
//import com.vmware.tanzu.springcloudgateway.apis.TanzuVmwareComV1Api;
//import com.vmware.tanzu.springcloudgateway.models.V1SpringCloudGatewayMapping;
//import io.kubernetes.client.openapi.ApiException;
//import io.kubernetes.client.openapi.models.V1ObjectMeta;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//
//import static com.vmware.tanzu.springcloudgateway.mapping.MappingFinalizerEditor.MAPPING_FINALIZER_STRING;
//
//@KubernetesComponentTest
//class CatFinalizerEditorComponentTest {
//
//	@Value("${operator.install-namespace}") private String TEST_NAMESPACE;
//
//	@Autowired private TanzuVmwareComV1Api mappingV1Api;
//	@Autowired private MappingFinalizerEditor finalizerEditor;
//	@Autowired private TestK8SClient testK8SClient;
//
//	private String mappingName;
//
//	@AfterAll
//	void deleteKubernetesResources() throws ApiException {
//		testK8SClient.deleteAllKubectl();
//	}
//
//	@BeforeEach
//	void generateMappingName() {
//		mappingName = "mapping-" + UUID.randomUUID().toString();
//	}
//
//	@AfterEach
//	void deleteMapping() {
//		testK8SClient.deleteMapping(TEST_NAMESPACE, mappingName);
//	}
//
//	@Test
//	void add_shouldAddMappingFinalizerToMappingResource() throws ApiException, FileNotFoundException {
//		V1SpringCloudGatewayMapping existing = testK8SClient.createMapping(TEST_NAMESPACE, mappingName);
//		V1SpringCloudGatewayMapping returned = finalizerEditor.add(existing);
//
//		assertThat(returned.getMetadata().getFinalizers()).contains(MAPPING_FINALIZER_STRING);
//
//		V1SpringCloudGatewayMapping mappingFromApiServer = mappingV1Api.readNamespacedSpringCloudGatewayMapping(
//				mappingName, TEST_NAMESPACE, null, null);
//		assertThat(mappingFromApiServer.getMetadata().getFinalizers()).contains(MAPPING_FINALIZER_STRING);
//	}
//
//	@Test
//	void remove_shouldRemoveMappingFinalizerToMappingResource() throws FileNotFoundException, ApiException {
//		V1ObjectMeta metadata = new V1ObjectMeta().name(mappingName)
//												  .addFinalizersItem(MAPPING_FINALIZER_STRING);
//		V1SpringCloudGatewayMapping existing = testK8SClient.createMappingWithMetadata(TEST_NAMESPACE, metadata);
//		V1SpringCloudGatewayMapping returned = finalizerEditor.remove(existing);
//
//		assertThat(returned.getMetadata().getFinalizers()).isNull();
//
//		V1SpringCloudGatewayMapping mappingFromApiServer = mappingV1Api.readNamespacedSpringCloudGatewayMapping(
//				mappingName, TEST_NAMESPACE, null, null);
//		assertThat(mappingFromApiServer.getMetadata().getFinalizers()).isNull();
//	}
//}
