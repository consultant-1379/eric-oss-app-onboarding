#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

stage('Custom Generate Docs') {
    sh "${bob} -r ${ruleset} generate-docs"
    publishHTML(target: [
            allowMissing         : false,
            alwaysLinkToLastBuild: false,
            keepAll              : true,
            reportDir            : 'src/main/resources/v1',
            reportFiles          : 'index.html',
            reportName           : 'REST API Documentation'
    ])
}

stage('Open API Spec') {
    sh "${bob} rest-2-html:check-has-open-api-been-modified"
    script {
        def val = readFile '.bob/var.has-openapi-spec-been-modified'
        if (val.trim().equals("true")) {
            sh "${bob} rest-2-html:zip-open-api-doc"
            sh "${bob} rest-2-html:generate-html-output-files"
            manager.addInfoBadge("OpenAPI spec has changed. Review the Archived HTML Output files: rest2html*.zip")
            archiveArtifacts allowEmptyArchive: true, artifacts: "rest_conversion_log.txt, rest2html*.zip"
            echo "Sending email to CPI document reviewers distribution list: ${env.CPI_DOCUMENT_REVIEWERS_DISTRIBUTION_LIST}"
            try {
                mail to: "${env.CPI_DOCUMENT_REVIEWERS_DISTRIBUTION_LIST}",
                        from: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                        cc: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                        subject: "[${env.JOB_NAME}] OpenAPI specification has been updated and is up for review",
                        body: "The OpenAPI spec documentation has been updated.<br><br>" +
                                "Please review the patchset and archived HTML output files (rest2html*.zip) linked here below:<br><br>" +
                                "&nbsp;&nbsp;Gerrit Patchset: ${env.GERRIT_CHANGE_URL}<br>" +
                                "&nbsp;&nbsp;HTML output files: ${env.BUILD_URL}artifact <br><br><br><br>" +
                                "<b>Note:</b> This mail was automatically sent as part of the following Jenkins job: ${env.BUILD_URL}",
                        mimeType: 'text/html'
            } catch (Exception e) {
                echo "Email notification was not sent."
                print e
            }
        }
    }
}

stage('Custom Hooklauncher Image') {
    echo 'Build Hooklauncher image:'
    sh "${bob} -r ${ruleset} build-hooklauncher-image"
}

stage('Custom Python Image') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
        echo 'Build Python Scripts Image:'
        if (env.RELEASE) {
            sh "${bob} -r ${ruleset} python-image-drop-repo"
        } else {
            sh "${bob} -r ${ruleset} python-image-ci-internal-repo"
        }
    }
}

stage('Custom Package') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
        sh "${bob} -r ${ruleset} package"
    }
}