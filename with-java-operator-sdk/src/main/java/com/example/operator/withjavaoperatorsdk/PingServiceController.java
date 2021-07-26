package com.example.operator.withjavaoperatorsdk;

import java.util.Collections;

import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class PingServiceController implements ResourceController<PingService> {

    //TODO: more complete sample: https://github.com/java-operator-sdk/java-operator-sdk/blob/112a8268155a9f6262e1238b592e9813b09e1557/operator-framework/src/test/java/io/javaoperatorsdk/operator/sample/simple/TestCustomResourceController.java
    public static final String KIND = "PingService";
    private static final Logger log = LoggerFactory.getLogger(PingServiceController.class);

    private final KubernetesClient kubernetesClient;

    public PingServiceController(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public DeleteControl deleteResource(PingService resource, Context<PingService> context) {
        log.info("Execution deleteResource for: {}", resource.getMetadata().getName());
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PingService> createOrUpdateResource(
            PingService resource, Context<PingService> context) {
        log.info("Execution createOrUpdateResource for: {}", resource.getMetadata().getName());

        ServicePort servicePort = new ServicePort();
        servicePort.setPort(8080);
        ServiceSpec serviceSpec = new ServiceSpec();
        serviceSpec.setPorts(Collections.singletonList(servicePort));

//   kubernetesClient
//       .services()
//       .inNamespace(resource.getMetadata().getNamespace())
//       .createOrReplace(
//           new ServiceBuilder()
//               .withNewMetadata()
//               .withName(resource.getSpec().getExternalName())
//               .endMetadata()
//               .withSpec(serviceSpec)
//               .build());
        return UpdateControl.updateCustomResource(resource);
    }
}
