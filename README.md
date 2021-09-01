# kubernetes-java-operator-sample

Sample operators leveraging [Kubernetes Java Client](https://github.com/kubernetes-client/java).

## With Kubernetes Java Client

### Deployment

The easiest way to get started is with [skaffold](https://skaffold.dev/):

```bash
skaffold run # build and deploy and forget
skaffold delete # delete everything deployed with `skaffold run`
```

If you'd like to ask skaffold to watch changes and rebuild or redeploy, you may run:
```bash
skaffold dev # watch changes and automatically update deployment, tear down when stop the process.
```

If you want to skip image building and just deploy this sample operator to your cluster, you may run:

```bash
kubectl apply -f crds/adoption-center-custom-resource-definition.yaml
kubectl apply -f crds/cat-custom-resource-definition.yaml
kustomize build manifests | kubectl apply -f -
```

The operator resources will all be deployed to namespace `adoption-operator` as specified in [manifests/operator-namespace.yaml](manifests/operator-namespace.yaml).

### Try it out

You may apply a test adoption center and see an adoption center deployment shows up in the operator namespace `adoption-operator` with the name specified in [manifests/test-adoption-center.yaml](manifests/test-adoption-center.yaml):

```bash
kubectl apply -f manifests/test-adoption-center.yaml
kubectl get deployments -n adoption-operator
kubectl port-forward deployment.apps/kittens-dream-land 8080:8080
```

Then you can pull the `/animals` endpoint from the adoption center:

```bash
curl localhost:8080/animals # Should receive `[]` without applying a cat resource.
```

You may apply a test cat resource and see the cat show up in the adoption center `/animals` endpoint:

```bash
kubectl apply -f manifests/test-cat.yaml
kubectl -n adoption-operator wait --for=condition=ready pod/kittens-dream-land-${randomGeneratedHash}
curl localhost:8080/animals # Should see the new cat in the response
```

### Generating Java Classes from CRD

```bash
./with-kubernetes-java-client/crds/generate-models-for-crd.sh
```

THis script applies the CRDs to a kubernetes cluster, pulls the `openapi` endpoint of the API server, and generate classes based on the result there. 

More information about the generator module can be found [here](https://github.com/building-k8s-operator/kubernetes-java-operator-sample#generating-java-classes-from-crd)  

### Run tests

To run the Component tests, it's better to use a local development cluster. In this example we are using [kind](https://kind.sigs.k8s.io/docs/user/quick-start/):

```bash
kind create cluster
./gradlew test
```

## File Structure

You will notice the following directory structure

```text
├── README.md
├── skaffold.yaml
├── crds
│   ├── adoption-center-custom-resource-definition.yaml
│   ├── cat-custom-resource-definition.yaml
│   └── generate-models-for-crd.sh
├── manifests
│   ├── kustomization.yaml
│   ├── operator-*.yaml
│   ├── test-adoption-center.yaml
│   └── test-cat.yaml
├── with-kubernetes-java-client
│   ├── build.gradle.kts
│   └── src
│       └── ...
└── adoption-center
    ├── build.gradle.kts
    └── src
        └── ...
```
