#
# COPYRIGHT Ericsson 2021
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

ARG CBOS_IMAGE_TAG
ARG CBOS_IMAGE_REPO="armdocker.rnd.ericsson.se/proj-ldc/common_base_os_release"
ARG CBOS_IMAGE_NAME="sles"
FROM ${CBOS_IMAGE_REPO}/${CBOS_IMAGE_NAME}:${CBOS_IMAGE_TAG}

ARG CBOS_IMAGE_TAG
ARG CBOS_IMAGE_NAME="sles"
ARG CBOS_REPO_URL=https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/${CBOS_IMAGE_NAME}/${CBOS_IMAGE_TAG}

ARG SELI_ARTIFACTORY_REPO_USER
ARG SELI_ARTIFACTORY_REPO_PASS

WORKDIR /usr/src/app

COPY requirements .

RUN zypper ar -C -G -f $CBOS_REPO_URL?ssl_verify=no \
   COMMON_BASE_OS_SLES_REPO \
   && zypper install -l -y curl \
   && zypper install -l -y postgresql \
   && zypper install -l -y python3 \
   && zypper install -l -y python3-psycopg2 \
   && zypper install -l -y python3-pip \
   && python3 -m pip install -r requirements --no-cache-dir \
   && zypper clean --all \
   && zypper rr COMMON_BASE_OS_SLES_REPO

ENV PYTHONUNBUFFERED=1
COPY . .


# Download kubectl binary for later inclusion in final image.
WORKDIR /usr/bin
RUN curl -s -u ${SELI_ARTIFACTORY_REPO_USER}:${SELI_ARTIFACTORY_REPO_PASS} -O https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-thirdparty-generic-local/io/confluent/eric-oss-app-onboarding/kubectl-1.29.2.zip

RUN python3 -m zipfile -e kubectl-1.29.2.zip . \
    && chmod +x kubectl \
    && rm kubectl-1.29.2.zip

## Make sure kubectl is executable
RUN kubectl --help

WORKDIR /usr/src/app
