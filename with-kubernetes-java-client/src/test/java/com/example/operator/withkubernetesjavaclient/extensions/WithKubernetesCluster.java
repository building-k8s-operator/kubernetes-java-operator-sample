/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.operator.withkubernetesjavaclient.extensions;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import org.junit.jupiter.api.TestInstance;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest(classes = WithKubernetesCluster.TestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface WithKubernetesCluster {

	@org.springframework.boot.test.context.TestConfiguration
	class TestConfiguration {

		@Bean
		ApiClient apiClient() throws IOException {
			return ClientBuilder.defaultClient();
		}
		@Bean
		TestK8sClient testK8SClient(ApiClient apiClient) throws IOException {
			return new TestK8sClient(apiClient);
		}
	}

}
