{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-app-onboarding.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-app-onboarding.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-app-onboarding.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-app-onboarding.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-app-onboarding.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-app-onboarding.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-app-onboarding.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-app-onboarding" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-app-onboarding" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-app-onboarding" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-app-onboarding" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-app-onboarding") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-app-onboarding" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-app-onboarding" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-app-onboarding" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-app-onboarding" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-app-onboarding" "repoPath") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.kubeclientImage" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "kubeclient" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "kubeclient" "repoPath") -}}
    {{- $name := (index $productInfo "images" "kubeclient" "name") -}}
    {{- $tag := (index $productInfo "images" "kubeclient" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "kubeclient") -}}
            {{- if (index .Values "imageCredentials" "kubeclient" "registry") -}}
                {{- if (index .Values "imageCredentials" "kubeclient" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "kubeclient" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "kubeclient" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "kubeclient" "repoPath") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.testImagePath" }}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-app-onboardingTest" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-app-onboardingTest" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-app-onboardingTest" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-app-onboardingTest" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
                {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-app-onboardingTest") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-app-onboardingTest" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-app-onboardingTest" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-app-onboardingTest" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
            {{- if not (kindIs "invalid" (index .Values "imageCredentials" "eric-oss-app-onboardingTest" "repoPath")) -}}
                {{- $repoPath = (index .Values "imageCredentials" "eric-oss-app-onboardingTest" "repoPath") -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "%s/%s%s:%s" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-app-onboarding.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Standard labels of Helm and Kubernetes
*/}}
{{- define "eric-oss-app-onboarding.standard-labels" }}
app.kubernetes.io/name: {{ include "eric-oss-app-onboarding.name" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ include "eric-oss-app-onboarding.version" . }}
helm.sh/chart: {{ include "eric-oss-app-onboarding.chart" . }}
eric-data-object-storage-mn-access: "true"
chart: {{ include "eric-oss-app-onboarding.chart" . }}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-app-onboarding.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-app-onboarding.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

Create label for Log Shipper sidecar
*/}}
{{ define "eric-oss-app-onboarding.log-shipper-labels" }}
  {{- if has "stream" .Values.log.outputs }}
    {{ .Values.logShipper.logtransformer.host }}-access: "true"
  {{- end }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard, Config and Log Shipper
*/}}
{{- define "eric-oss-app-onboarding.labels" -}}
  {{- $standard := include "eric-oss-app-onboarding.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-app-onboarding.config-labels" . | fromYaml -}}
  {{- $logshipper := include "eric-oss-app-onboarding.log-shipper-labels" . | fromYaml -}}
  {{- include "eric-oss-app-onboarding.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config $logshipper)) | trim }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-app-onboarding.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if .Values.global.fsGroup.namespace -}}
          {{- if eq .Values.global.fsGroup.namespace true -}}
            # The 'default' defined in the Security Policy will be used.
          {{- else -}}
            10000
          {{- end -}}
        {{- else -}}
          10000
        {{- end -}}
      {{- end -}}
    {{- else -}}
      10000
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "eric-oss-app-onboarding.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-app-onboarding.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
eric-data-object-storage-mn-access:true
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-app-onboarding.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-app-onboarding.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create annotation for the product information (DR-D1121-064, DR-D1121-067)
*/}}
{{- define "eric-oss-app-onboarding.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{ regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-oss-app-onboarding.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-app-onboarding.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-app-onboarding.annotations" -}}
  {{- $productInfo := include "eric-oss-app-onboarding.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-app-onboarding.config-annotations" . | fromYaml -}}
  {{- $prometheusAnn := include "eric-oss-app-onboarding.prometheus" . | fromYaml -}}
  {{- include "eric-oss-app-onboarding.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $prometheusAnn $config)) | trim }}
{{- end -}}

{{/*
Merged annotations without prometheus, which includes only productInfo and config
*/}}
{{- define "eric-oss-app-onboarding.annotations-without-prometheus" -}}
  {{- $productInfo := include "eric-oss-app-onboarding.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-app-onboarding.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-app-onboarding.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}

{{/*
Merged annotations for Default, which includes productInfo and config
*/}}
{{- define "eric-oss-app-onboarding.annotations-rbac" -}}
  {{- $productInfo := include "eric-oss-app-onboarding.product-info" . | fromYaml -}}
  {{- $config := include "eric-oss-app-onboarding.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-app-onboarding.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
helm.sh/resource-policy: keep
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-app-onboarding.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
prometheus.io/scrape-role: {{ .Values.prometheus.role | quote }}
prometheus.io/scrape-interval: {{ .Values.prometheus.interval | quote }}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-app-onboarding.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-app-onboarding.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-app-onboarding.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-app-onboarding.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector.onboarding -}}
  {{- range $key, $localValue := .Values.nodeSelector.onboarding -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    {{- toYaml (merge $globalValue .Values.nodeSelector.onboarding) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
The name of the cluster role used during Openshift deployments.
This helper is provided to allow use of the new global.security.privilegedPolicyClusterRoleName if set, otherwise
use the previous naming convention of <release_name>-allowed-use-privileged-policy for backwards compatibility.
*/}}
{{- define "eric-oss-app-onboarding.privileged.cluster.role.name" -}}
{{- $privilegedClusterRoleName := printf "%s%s" (include "eric-oss-app-onboarding.name" . ) "-allowed-use-privileged-policy" -}}
{{- if .Values.global -}}
  {{- if .Values.global.security -}}
    {{- if hasKey (.Values.global.security) "privilegedPolicyClusterRoleName" -}}
      {{- $privilegedClusterRoleName = .Values.global.security.privilegedPolicyClusterRoleName }}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- $privilegedClusterRoleName -}}
{{- end -}}

{{/*
check global.security.tls.enabled
*/}}
{{- define "eric-oss-app-onboarding.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
      {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
      {{- "false" -}}
    {{- end -}}
  {{- else -}}
    {{- "false" -}}
  {{- end -}}
{{- else -}}
  {{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
DR-D470217-007-AD This helper defines whether this service enter the Service Mesh or not.
*/}}
{{- define "eric-oss-app-onboarding.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}

{{/*
DR-D470217-011 This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-oss-app-onboarding.service-mesh-inject" }}
{{- if eq (include "eric-oss-app-onboarding.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
{{- else -}}
sidecar.istio.io/inject: "false"
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation which adds hooks to delay application startup until the pod proxy is ready to accept traffic.
*/}}
{{- define "eric-oss-app-onboarding.istio-proxy-config-annotation" }}
{{- if eq (include "eric-oss-app-onboarding.service-mesh-enabled" .) "true" }}
proxy.istio.io/config: '{ "holdApplicationUntilProxyStarts": true }'
{{- end -}}
{{- end -}}

{{/*
GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-oss-app-onboarding.service-mesh-version" }}
{{- if eq (include "eric-oss-app-onboarding.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{ .Values.global.serviceMesh.annotations | toYaml }}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume
*/}}
{{- define "eric-oss-app-onboarding.service-mesh-volume" }}
{{- if and (eq (include "eric-oss-app-onboarding.service-mesh-enabled" .) "true") (eq (include "eric-oss-app-onboarding.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"eric-oss-app-onboarding-helm-chart-reg-certs-tls":{"secret":{"secretName":"eric-oss-app-onboarding-eric-lcm-helm-chart-registry-secret","optional":true}},"eric-oss-app-onboarding-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}},"app-onboarding-appmgr-data-pg-db-certs-tls":{"secret":{"secretName":"eric-oss-app-onboarding-data-pg-db-secret","optional":true}},"eric-oss-app-onboarding-eric-lcm-container-registry-tls":{"secret":{"secretName":"eric-oss-app-onboarding-eric-lcm-container-registry-secret","optional":true}},"eric-oss-app-onboarding-eric-data-object-storage-mn-tls":{"secret":{"secretName":"eric-oss-app-onboarding-eric-data-object-storage-mn-secret","optional":true}},"eric-oss-app-onboarding-eric-dst-collector-certs-tls":{"secret":{"secretName":"eric-oss-app-onboarding-eric-dst-collector-secret","optional":true}}}'
sidecar.istio.io/userVolumeMount: '{"eric-oss-app-onboarding-helm-chart-reg-certs-tls":{"mountPath":"/etc/istio/tls/eric-lcm-helm-chart-registry","readOnly":true},"eric-oss-app-onboarding-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true},"app-onboarding-appmgr-data-pg-db-certs-tls":{"mountPath":"/etc/istio/tls/appmgr-data-document-db/","readOnly":true},"eric-oss-app-onboarding-eric-lcm-container-registry-tls":{"mountPath":"/etc/istio/tls/eric-lcm-container-registry","readOnly":true},"eric-oss-app-onboarding-eric-data-object-storage-mn-tls":{"mountPath":"/etc/istio/tls/eric-data-object-storage-mn","readOnly":true},"eric-oss-app-onboarding-eric-dst-collector-certs-tls":{"mountPath":"/etc/istio/tls/eric-dst-collector/","readOnly":true}}'
{{ end }}
{{- end -}}

{{- define "eric-oss-app-onboarding-eric-lcm-container-registry-secret" }}
apiVersion: siptls.sec.ericsson.com/v1
kind: InternalCertificate
metadata:
  name: {{ include "eric-oss-app-onboarding.name" . }}-eric-lcm-container-registry-int-cert
  labels:
  {{- include "eric-oss-app-onboarding.labels" .| nindent 4 }}
  annotations:
  {{- include "eric-oss-app-onboarding.product-info" .| nindent 4 }}
spec:
  kubernetes:
    generatedSecretName: {{ include "eric-oss-app-onboarding.name" . }}-eric-lcm-container-registry-secret
    certificateName: "cert.pem"
    privateKeyName: "key.pem"
  certificate:
    subject:
      cn: {{ include "eric-oss-app-onboarding.name" . }}
    issuer:
      reference: eric-lcm-container-registry-client-ca
    extendedKeyUsage:
      tlsClientAuth: true
      tlsServerAuth: false
{{- end -}}

{{- define "eric-oss-app-onboarding-eric-lcm-helm-chart-registry-secret" }}
apiVersion: siptls.sec.ericsson.com/v1
kind: InternalCertificate
metadata:
  name: {{ include "eric-oss-app-onboarding.name" . }}-eric-lcm-helm-chart-registry-int-cert
  labels:
  {{- include "eric-oss-app-onboarding.labels" .| nindent 4 }}
  annotations:
  {{- include "eric-oss-app-onboarding.product-info" .| nindent 4 }}
spec:
  kubernetes:
    generatedSecretName: {{ include "eric-oss-app-onboarding.name" . }}-eric-lcm-helm-chart-registry-secret
    certificateName: "cert.pem"
    privateKeyName: "key.pem"
  certificate:
    subject:
      cn: {{ include "eric-oss-app-onboarding.name" . }}
    issuer:
      reference: eric-lcm-helm-chart-registry-client-ca
    extendedKeyUsage:
      tlsClientAuth: true
      tlsServerAuth: false
{{- end -}}

{{/*
This helper defines which out-mesh services are reached by the eric-oss-app-onboarding.
*/}}
{{- define "eric-oss-app-onboarding.service-mesh-ism2osm-labels" }}
{{- if eq (include "eric-oss-app-onboarding.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-oss-app-onboarding.global-security-tls-enabled" .) "true" }}
eric-lcm-container-registry-ism-access: "true"
eric-lcm-helm-chart-registry-ism-access: "true"
eric-appmgr-data-document-db: "true"
eric-data-object-storage-mn-ism-access: "true"
eric-dst-collector-ism-access: "true"
  {{- end }}
{{- end -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-app-onboarding.directStreamingLabel" -}}
{{- if eq (include "eric-oss-app-onboarding.log-streaming-activated" .) "true" }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}


{{/*
Define the secret key reference for Object Storage
*/}}
{{- define "eric-oss-app-onboarding.secret-key-reference" }}
{{- if (lookup "v1" "Secret" .Release.Namespace "eric-data-object-storage-mn-secret") }}
    {{- print "eric-data-object-storage-mn-secret" -}}
{{- else }}
    {{- print "eric-eo-object-store-cred" -}}
{{- end -}}
{{- end -}}

{{/*
Define logging environment variables (DR-470222-010)
*/}}
{{ define "eric-oss-app-onboarding.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-app-onboarding.log-streamingMethod" .) }}
- name: LOGS_STREAMING_METHOD
  value: {{ $streamingMethod | quote }}
{{- if eq (include "eric-oss-app-onboarding.log-streaming-activated" .) "true" }}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-http.xml"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual.xml"
  {{- end }}
- name: LOGSTASH_DESTINATION
  value: eric-log-transformer
- name: LOGSTASH_PORT
  value: "9080"
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-app-onboarding
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}

{{/*
This helper get the custom user used to connect to postgres DB instance.
*/}}
{{ define "eric-oss-app-onboarding.dbuser" }}
  {{- $secret := (lookup "v1" "Secret" .Release.Namespace "eric-appmgr-data-document-db-credentials") -}}
  {{- if $secret -}}
    {{ index $secret.data "custom-user" | b64dec | quote }}
  {{- else -}}
    {{- (randAlphaNum 16) | b64enc | quote -}}
  {{- end -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.podSecurityContext.supplementalGroups" -}}
  {{- $globalGroups :=  (list) -}}
  {{- if .Values.global -}}
      {{- if .Values.global.podSecurityContext -}}
          {{- if .Values.global.podSecurityContext.supplementalGroups -}}
              {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
          {{- end -}}
      {{- end -}}
  {{- end -}}
  {{- $localGroups := (list) -}}
  {{- if .Values.podSecurityContext -}}
      {{- if .Values.podSecurityContext.supplementalGroups -}}
          {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
      {{- end -}}
  {{- end -}}
  {{- $mergeGroups := (list) -}}
  {{- if $globalGroups -}}
      {{- $mergeGroups = $globalGroups -}}
  {{- end -}}
  {{- if $localGroups -}}
      {{- $mergeGroups = concat $mergeGroups $localGroups | uniq -}}
  {{- end -}}
  {{- if $mergeGroups -}}
      {{- toYaml $mergeGroups | nindent 8 -}}
  {{- end -}}
{{- end -}}

{{/*
Define tolerations
*/}}
{{- define "eric-oss-app-onboarding.tolerations" -}}
{{- $tolerations := list -}}
{{- if .Values.tolerations -}}
  {{- if ne (len .Values.tolerations) 0 -}}
    {{- range $t := .Values.tolerations -}}
      {{- $tolerations = append $tolerations $t -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.global -}}
  {{- if .Values.global.tolerations -}}
    {{- if ne (len .Values.global.tolerations) 0 -}}
      {{- range $t := .Values.global.tolerations -}}
        {{- $tolerations = append $tolerations $t -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{ toYaml $tolerations }}
{{- end -}}

{{/*
Define the log streaming method
*/}}
{{- define "eric-oss-app-onboarding.log-streamingMethod" -}}
  {{- $streamingMethod := "indirect" -}}
  {{- if ((.Values.log).streamingMethod) -}}
    {{- $streamingMethod = .Values.log.streamingMethod -}}
  {{- else if (((.Values.global).log).streamingMethod) -}}
    {{- $streamingMethod = .Values.global.log.streamingMethod -}}
  {{- else if ((.Values.log).outputs) -}}
    {{- if and (has "stdout" .Values.log.outputs) (has "stream" .Values.log.outputs) -}}
    {{- $streamingMethod = "dual" -}}
    {{- else if has "stream" .Values.log.outputs -}}
    {{- $streamingMethod = "direct" -}}
    {{- else if has "stdout" .Values.log.outputs -}}
    {{- $streamingMethod = "indirect" -}}
    {{- end -}}
  {{- else if (((.Values.global).log).outputs) -}}
    {{- if and (has "stdout" .Values.global.log.outputs) (has "stream" .Values.global.log.outputs) -}}
    {{- $streamingMethod = "dual" -}}
    {{- else if has "stream" .Values.global.log.outputs -}}
    {{- $streamingMethod = "direct" -}}
    {{- else if has "stdout" .Values.global.log.outputs -}}
    {{- $streamingMethod = "indirect" -}}
    {{- end -}}
  {{- end -}}
  {{- printf "%s" $streamingMethod -}}
{{- end -}}

{{/*
Define whether the log streaming is activated
*/}}
{{- define "eric-oss-app-onboarding.log-streaming-activated" }}
  {{- $streamingMethod := (include "eric-oss-app-onboarding.log-streamingMethod" .) -}}
  {{- if or (eq $streamingMethod "dual") (eq $streamingMethod "direct") -}}
    {{- printf "%t" true -}}
  {{- else -}}
    {{- printf "%t" false -}}
  {{- end -}}
{{- end -}}

{{/*
Define the Log Shipper container
*/}}
{{- define "eric-oss-app-onboarding.log-shipper-sidecar-container" }}
  {{- $logshipperImageDict := dict "logshipperSidecarImage" ((((.Values).global).logShipper).config).image -}}
  {{- include "eric-log-shipper-sidecar.log-shipper-sidecar-container" (mergeOverwrite . $logshipperImageDict ) }}
{{- end -}}

{{/*
Define the Log Shipper volume
*/}}
{{- define "eric-oss-app-onboarding.log-shipper-sidecar-volume" }}
- name: fluentbit-config
  configMap:
    name: eric-oss-app-onboarding-log-shipper-sidecar
    items:
      - key: fluent-bit.conf
        path: fluent-bit.conf
      - key: inputs.conf
        path: inputs.conf
      - key: outputs.conf
        path: outputs.conf
      - key: filters.conf
        path: filters.conf
      - key: parsers.conf
        path: parsers.conf
{{- end }}

{{- define "eric-oss-app-onboarding-eric-data-object-storage-mn-secret" }}
apiVersion: siptls.sec.ericsson.com/v1
kind: InternalCertificate
metadata:
  name: {{ include "eric-oss-app-onboarding.name" . }}-eric-data-object-storage-mn-int-cert
  labels:
  {{- include "eric-oss-app-onboarding.labels" .| nindent 4 }}
  annotations:
  {{- include "eric-oss-app-onboarding.product-info" .| nindent 4 }}
spec:
  kubernetes:
    generatedSecretName: {{ include "eric-oss-app-onboarding.name" . }}-eric-data-object-storage-mn-secret
    certificateName: "cert.pem"
    privateKeyName: "key.pem"
  certificate:
    subject:
      cn: {{ include "eric-oss-app-onboarding.name" . }}
    issuer:
      reference: eric-data-object-storage-mn-ca
    extendedKeyUsage:
      tlsClientAuth: true
      tlsServerAuth: true
{{- end -}}

{{/*
Define RoleBinding (DR-1123-134)
*/}}
{{- define "eric-oss-app-onboarding.securityPolicy.rolekind" -}}
  {{- $roleKind := "" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.securityPolicy -}}
      {{- if .Values.global.securityPolicy.rolekind -}}
        {{- if or (eq "Role" (.Values.global.securityPolicy).rolekind) (eq "ClusterRole" (.Values.global.securityPolicy).rolekind) -}}
          {{- $roleKind = .Values.global.securityPolicy.rolekind -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- print $roleKind -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.securityPolicy.rolename" -}}
{{- $rolename := (include "eric-oss-app-onboarding.name" .) -}}
{{- if .Values.securityPolicy -}}
  {{- if .Values.securityPolicy.rolename -}}
      {{- $rolename = .Values.securityPolicy.rolename -}}
  {{- end -}}
{{- end -}}
{{- $rolename -}}
{{- end -}}

{{/*
Create Security Policy RoleBinding name for ServiceAccount
The format for a rolebinding name is: <service account name>-<c|r>-<role name>-sp
*/}}
{{- define "eric-oss-app-onboarding.securityPolicy-rolebinding-name" -}}
{{- $serviceAccountName := include "eric-oss-app-onboarding.serviceAccountName" . -}}
{{- $rolecipher := substr 0 1 (include "eric-oss-app-onboarding.securityPolicy.rolekind" . | toString | lower) -}}
{{- $rolename := include "eric-oss-app-onboarding.securityPolicy.rolename" . -}}
{{- printf "%s-%s-%s-sp" $serviceAccountName $rolecipher $rolename -}}
{{- end -}}

{{- define "eric-oss-app-onboarding.registryUrl" -}}
  {{- $g := fromJson (include "eric-oss-app-onboarding.global" .) -}}
  {{- $registryUrl := $g.registry.url -}}
  {{- if .Values.global -}}
    {{- if .Values.global.registry -}}
      {{- if .Values.global.registry.url -}}
        {{- $registryUrl = .Values.global.registry.url -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $registryUrl }}
{{- end -}}

{{- define "eric-oss-app-onboarding.imagePullPolicy" -}}
  {{- $g := fromJson (include "eric-oss-app-onboarding.global" .) -}}
  {{- $imagePullPolicy := $g.registry.imagePullPolicy -}}
  {{- if .Values.global -}}
    {{- if .Values.global.registry -}}
      {{- if .Values.global.registry.imagePullPolicy -}}
        {{- $imagePullPolicy = .Values.global.registry.imagePullPolicy -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- $imagePullPolicy }}
{{- end -}}

{{/*
check global.eric-oss-app-onboarding-stub.enabled
*/}}
{{- define "eric-oss-app-onboarding.app-onboarding-stub-enabled" -}}
{{- $g := fromJson (include "eric-oss-app-onboarding.global" .) -}}
{{- $stubEnabled := (index $g "eric-oss-app-onboarding-stub" "enabled") -}}
{{- if .Values.global -}}
  {{- if (index .Values "global" "eric-oss-app-onboarding-stub") -}}
    {{- if (index .Values "global" "eric-oss-app-onboarding-stub" "enabled") -}}
      {{- $stubEnabled = (index .Values "global" "eric-oss-app-onboarding-stub" "enabled") -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- $stubEnabled -}}
{{- end -}}

{{/*
Create a map from ".Values.global" with defaults if missing in values file.
This hides defaults from values file.
*/}}
{{ define "eric-oss-app-onboarding.global" }}
  {{- $globalDefaults := dict "registry" (dict "url" "armdocker.rnd.ericsson.se") -}}
  {{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "imagePullPolicy" "IfNotPresent")) -}}
  {{- $globalDefaults := merge $globalDefaults (dict "eric-oss-app-onboarding-stub" (dict "enabled" true)) -}}
  {{ if .Values.global }}
    {{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
  {{ else }}
    {{- $globalDefaults | toJson -}}
  {{ end }}
{{ end }}

{{/*
This helper defines whether DST is enabled or not.
*/}}
{{- define "eric-oss-app-onboarding.dst-enabled" }}
  {{- $dstEnabled := "false" -}}
    {{- if .Values.dst.enabled -}}
        {{- $dstEnabled = .Values.dst.enabled -}}
    {{- end -}}
  {{- $dstEnabled -}}
{{- end -}}

{{/*
Define the labels needed for DST
*/}}
{{- define "eric-oss-app-onboarding.dstLabels" -}}
{{- if eq (include "eric-oss-app-onboarding.dst-enabled" .) "true" }}
eric-dst-collector-access: "true"
{{- end }}
{{- end -}}

{{/*
This helper defines which exporter port must be used depending on protocol
*/}}
{{- define "eric-oss-app-onboarding.exporter-port" }}
  {{- $dstExporterPort := .Values.dst.collector.portOtlpGrpc -}}
    {{- if .Values.dst.collector.protocol -}}
      {{- if eq .Values.dst.collector.protocol "http" -}}
        {{- $dstExporterPort = .Values.dst.collector.portOtlpHttp -}}
      {{- end -}}
    {{- end -}}
  {{- $dstExporterPort -}}
{{- end -}}

{{/*
Define DST environment variables
*/}}
{{ define "eric-oss-app-onboarding.dstEnv" }}
{{- if eq (include "eric-oss-app-onboarding.dst-enabled" .) "true" }}
- name: ERIC_TRACING_ENABLED
  value: "true"
- name: ERIC_PROPAGATOR_PRODUCE
  value: {{ .Values.dst.producer.type }}
- name: ERIC_EXPORTER_PROTOCOL
  value: '{{ .Values.dst.collector.protocol }}'
- name: ERIC_TRACING_POLING_INTERVAL
  value: '{{ .Values.dst.collector.polingInterval }}'
{{- if eq .Values.dst.collector.protocol "grpc"}}
- name: ERIC_EXPORTER_ENDPOINT
  value: {{ .Values.dst.collector.host }}:{{ include "eric-oss-app-onboarding.exporter-port" . }}
{{- else if eq .Values.dst.collector.protocol "http"}}
  value: {{ .Values.dst.collector.host }}:{{ include "eric-oss-app-onboarding.exporter-port" . }}/v1/traces
{{- end }}
- name: ERIC_SAMPLER_JAEGER_REMOTE_ENDPOINT
  value: {{ .Values.dst.collector.host }}:{{ .Values.dst.collector.portJaegerGrpc }}
{{- if eq .Values.dst.collector.protocol "http"}}
- name: OTEL_EXPORTER_OTLP_TRACES_PROTOCOL
  value: http/protobuf
{{- end }}
{{- else }}
- name: ERIC_TRACING_ENABLED
  value: "false"
{{- end -}}
{{ end }}
