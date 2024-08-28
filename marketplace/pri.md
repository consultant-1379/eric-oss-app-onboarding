
# App Onboarding PRI

APR 201 534, R1A
App Onboarding

## Revision History

| Revision | Date       | Reason for Revision | Reviewer |
|----------|------------|---------------------|----------|
| PA1 | 2022-01-10 | Fist draft | Andres Leal |
| PA2 | 2022-02-10 | Maturity Level increase and fixes | Andres Leal |
| PA3 | 2022-04-01 | Add Enable/disable App step | EZMURKI |

## Reason for Revision

New feature for Enable and Disable of Apps

## Evidence of Conformity with the Acceptance Criteria

The release criteria have been fulfilled.

The release decision has been taken by the approver of this document.

## Technical Solution

### Implemented Requirements

| REQUIREMENT ID (MR/JIRA ID) | HEADING/DESCRIPTION |
|-----------------------------|---------------------|
| IDUN-1944 | Introduce EO Onboarding Service & Catalog Manager for Apps |
| IDUN-9803 | App Onboarding : Service Maturity Ready for Non-Commercial Use |
| IDUN-13185 | App Manager: Enabling and disabling apps |

### Implemented additional features

No additional features implemented in this release

| JIRA-ID | JIRA HEADING/DESCRIPTION |
|---------|--------------------------|
| | |

### Implemented API Changes

New API for Enable/Disable implemented

### SW Library

No SW library product for this service.

| SW LIBRARY NAME | PRODUCT NUMBER | OLDEST COMPATIBLE VERSION |
|----------|------------|---------------------|
| | | |

### Reusable Images

No reusable images products for this service.

### Impact on Users: Abrupt NBC

No Abrupt NBC introduced in this release.

### Corrected Vulnerability Trouble Reports

No vulnerability trouble reports fixed in this release.

| Vulnerability ID(s) | Vulnerability Description | TR ID |
|----------|------------|---------------------|
| | | |

### Restrictions and Limitations

#### Exemptions

No exemption is present in this release.

#### Open Trouble Reports

No open Trouble Reports

| TR ID | TR HEADING | Priority |
|-------|------------|----------|
| | | |

#### Backward incompatibilities

No NBC introduced in this release.

#### Unsupported Upgrade/Rollback paths

No NUC/NRC introduced in this release.

| Oldest version for which upgrade to this release is supported | Oldest version for which rollback from this release is supported |
|---------|--------------------------|
| | |

#### Features not ready for commercial use

No Feature with Feature Maturity Alpha/Beta included in this release

## Product Deliverables

### Software Products

The following table shows the software products of this release.

| Product Type | Name | Product ID | New R-state | SHA256 Checksum |
|--------------|------|------------|-------------|-----------------|
| | | | | |

### New and Updated 2PP/3PP

(This section shall report all new and updated 2PPs and 3PPs integrated by the service.
This includes SW libraries and reusable images embedded as 2PP in the service.)

There are no new 2PP/3PP's as part of current revision.

The following 2PP/3PP’s are from previous revision:

| Name                      | Product ID    | Old Version | New Version   |
|---------------------------|---------------|-------------|---------------|
| jackson-databind-nullable | 4/CTX1027927  | 0.2.1       | 0.2.4         |
| jaeger-client-java        | 8/CTX1022999  | ---         | RELEASE-1.6.0 |
| swagger-annotations       | 11/CAX1056693 | ---         | 1.6.2         |
| PostgreSQL JDBC Driver    | 55/CAX1053319 | ---         | 42.2.23       |
| Lombok                    | 14/CAX1056250 | ---         | 1.18.16       |
| SnakeYAML                 | 23/CAX1056807 | ---         | 1.32          |

### Helm Chart Link

The following table shows the repository manager links for this release:

| RELEASE | HELM PACKAGE LINK |
|---------|--------------------------|
| App Onboarding R1A | <https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm-local/eric-oss-app-onboarding/eric-oss-app-onboarding-1.0.51-1.tgz>|

### Related Documents

All the documents can be accessible from Marketplace, see chapter “Product Documentation” for details.

## Product Documentation

### Developer Product Information

The Developer Product Information (DPI) documentation can be accessed using the following link:
<https://adp.ericsson.se/marketplace/app-onboarding>

### Customer Product Information

This service does not provide any CPI content.

## Deployment Information

The App Onboarding can be deployed in a Kubernetes environment.

### Deployment Instructions

The target group for the deployment instructions is only application developers and application integrators.
Deployment instruction can be found in the [App Onboarding User Guide][userguide].

### Upgrade Information

## Verification Status

The verification status will be described in the App Onboarding/ App manager Test Report.

### Stakeholder Verification

It is verified in ADP CICD pipeline as part of the ADP staging phase and in additional to application staging pipelines. See CI/CD Dashboard for further understanding.
Note: that result showing in this CI/CD Dashboard pipeline is always result of latest build and is not this specific PRA release.

## Support

For support use the Generic Services Support JIRA project, please also see the Service Name Service troubleshooting guidelines in Marketplace where you will find more detailed support information.

## References

[JIRA][jira]

[ADP Marketplace][marketplace]

[App Onboarding User Guide][userguide]

[jira]:<https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7787&view=reporting&chart=sprintRetrospective&sprint=32432>
[marketplace]:<https://adp.ericsson.se/marketplace/app-onboarding>
[userguide]:<https://adp.ericsson.se/marketplace/app-onboarding/documentation/development/additional-documents/user-guide>
