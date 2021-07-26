package com.example.operator.withjavaoperatorsdk;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public KubernetesClient kubernetesClient() {
    return new DefaultKubernetesClient();
  }

  @Bean
  public PingServiceController customServiceController(KubernetesClient client) {
    return new PingServiceController(client);
  }

  //  Register all controller beans
  @Bean
  public Operator operator(KubernetesClient client, List<ResourceController> controllers) {
    Operator operator = new Operator(client, DefaultConfigurationService.instance());
    controllers.forEach(operator::register);
    return operator;
  }
}
