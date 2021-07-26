package com.example.operator.withjavaoperatorsdk;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("sample.operator.java")
@Version("v1alpha1")
@ShortNames("ping")
@Plural("pingservices")
public class PingService extends CustomResource<PingSpec, PingStatus> implements Namespaced {}
