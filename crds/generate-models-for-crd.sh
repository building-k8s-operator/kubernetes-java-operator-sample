#!/bin/bash

set -euo pipefail

readonly CLIENT_GEN_DIR="/tmp/kubernetes-client-gen"
readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"

readonly CAT_CRD_MANIFEST_FILE=$PROJECT_ROOT/crds/cat-custom-resource-definition.yaml
readonly ADOPTION_CENTER_CRD_MANIFEST_FILE=$PROJECT_ROOT/crds/adoption-center-custom-resource-definition.yaml

readonly PACKAGE_NAME=com/example/operator/withkubernetesjavaclient
readonly GENERATED_SOURCES_PATH=with-kubernetes-java-client/src/generated/java/$PACKAGE_NAME

deleteExisting() {
  kubectl delete -f $CAT_CRD_MANIFEST_FILE || true
  kubectl delete -f $ADOPTION_CENTER_CRD_MANIFEST_FILE || true
  rm -Rf $CLIENT_GEN_DIR
  rm -Rf /tmp/gen-output
  rm -Rf /tmp/swagger
  rm -Rf $PROJECT_ROOT/$GENERATED_SOURCES_PATH/models/*
  rm -Rf $PROJECT_ROOT/$GENERATED_SOURCES_PATH/apis/*
}

applyCrd() {
  echo "Applying CRD to the cluster"
  kubectl apply -f $CAT_CRD_MANIFEST_FILE
  kubectl apply -f $ADOPTION_CENTER_CRD_MANIFEST_FILE
}

generate() {
  echo "Pulling down generator"
  git clone https://github.com/kubernetes-client/gen ${CLIENT_GEN_DIR} || true
  cd $CLIENT_GEN_DIR/openapi

  echo "Reading OpenAPI endpoint until CRD shows up"
  kubectl get --raw="/openapi/v2" > /tmp/swagger
  while ! (grep -Fq '"CatForAdoption"' /tmp/swagger && grep -Fq '"AdoptionCenter"' /tmp/swagger); do
    echo "Waiting for CRD to be applied..."
    sleep 1
    kubectl get --raw="/openapi/v2" > /tmp/swagger
  done

  echo "Generating models and clients"
  bash java-crd-cmd.sh -n com.example.operator -p com.example.operator.withkubernetesjavaclient -l 2 -h true -o /tmp/gen-output -g true < /tmp/swagger
}

copyToProject() {
  echo "Copying generated to project"
  cp -Rf /tmp/gen-output/src/main/java/$PACKAGE_NAME/models/ $PROJECT_ROOT/$GENERATED_SOURCES_PATH/models
  cp -Rf /tmp/gen-output/src/main/java/$PACKAGE_NAME/apis/   $PROJECT_ROOT/$GENERATED_SOURCES_PATH/apis
}

deleteExisting
applyCrd
generate
copyToProject
