# kubernetes-java-operator-sample

Sample operators leveraging [Kubernetes Java Client](https://github.com/kubernetes-client/java)

## With Kubernetes Java Client

To run the Component tests, you need a cluster local development cluster:

```bash
kind create cluster
```

To run the whole test suite, including acceptance tests, you to have the project deployed in the cluster:

```bash
skaffold run
```

To run the tests:

```bash
kind create cluster
```

### Generating Java Classes from CRD

```bash
./with-kubernetes-java-client/crds/generate-models-for-crd.sh
```
