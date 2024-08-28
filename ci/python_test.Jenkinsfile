#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Custom Python Test') {
    sh "${bob} -r ${ruleset} test-python"
}