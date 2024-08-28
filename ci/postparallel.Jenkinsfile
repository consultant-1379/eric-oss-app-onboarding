#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Custom Package Jars') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
        sh "${bob} -r ${ruleset} package-jars"
    }
}

stage('Custom Marketplace Upload') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),string(credentialsId: 'MARKETPLACE_TOKEN_APP_ONBOARDING', variable: 'MARKETPLACE_TOKEN_APP_ONBOARDING')]) {
        if (env.RELEASE) {
            sh "${bob} -r ${ruleset} marketplace-upload-release"
            sh "${bob} -r ${ruleset} marketplace-upload-refresh"
        }
        else {
            sh "${bob} -r ${ruleset} marketplace-upload-in-development"
        }
    }
}