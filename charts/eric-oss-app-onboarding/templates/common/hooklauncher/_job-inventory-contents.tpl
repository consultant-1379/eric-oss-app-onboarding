{{- define "eric-oss-app-onboarding.hkln.job-inventory-contents" -}}

- supportedSource: "<={{ .Chart.Version }}, >=1.0.182-5"
  supportedTarget: "{{ .Chart.Version }}"
  jobList:
    - weight: -202
      triggerWhen: ["pre-upgrade"]
      jobManifest: {{ include "eric-oss-app-onboarding.statefulsetDelete" . | fromYaml | toJson | b64enc | trim | nindent 8 }}

- supportedSource: "{{ .Chart.Version }}"
  supportedTarget: ">=1.0.182-5"
  jobList:
    - weight: -202
      triggerWhen: ["pre-rollback"]
      jobManifest: {{ include "eric-oss-app-onboarding.statefulsetDelete" . | fromYaml | toJson | b64enc | trim | nindent 8 }}
{{- end -}}
{{- define "eric-oss-app-onboarding.statefulsetDelete" -}}
apiVersion: batch/v1
kind: Job
metadata:
  name: {{ template "eric-oss-app-onboarding.name" . }}--job-delete-statefulset
  labels:
  {{- include "eric-oss-app-onboarding.labels" . | nindent 4 }}
  annotations:
  {{- include "eric-oss-app-onboarding.annotations" . | nindent 4 }}
spec:
  template:
    spec:
      nodeSelector: {{ include "eric-oss-app-onboarding.nodeSelector" . | nindent 6 -}}
      restartPolicy: Never
      serviceAccountName: {{ template "eric-oss-app-onboarding.name" . }}-hooklauncher-sa
      containers:
      - name: delete-statefulset
        securityContext: {}
        image: {{ include "eric-oss-app-onboarding.registryUrl" . }}/{{ .Values.pythonJob.image }}
        command:
        - /bin/sh
        - -c
        - |
          if ! kubectl get deployment eric-oss-app-onboarding
          then
          kubectl scale statefulsets {{ template "eric-oss-app-onboarding.name" . }} --replicas=0;
          kubectl delete statefulset {{ template "eric-oss-app-onboarding.name" . }};
          fi
  backoffLimit: 1
{{- end -}}
