# App Onboarding Use Cases

This section describes the following use cases for onboarding a CSAR App Package with EIAP SDK:

- [Onboard App CSAR](#developer-guide-use-cases?chapter=onboarding-app-CSAR)
- [Query Apps](#developer-guide-use-cases?chapter=query-apps)
- [Query an App](#developer-guide-use-cases?chapter=query-an-app)
- [Update an onboarded App](#developer-guide-use-cases?chapter=update-an-onboarded-app)
- [Get list of artifacts from an App](#developer-guide-use-cases?chapter=get-list-of-artifacts-from-an-app)
- [Get an artifact from an App](#developer-guide-use-cases?chapter=get-an-artifact-from-an-app)

## Onboard App CSAR

**Description**: Onboards an App in CSAR format to App Manager.

The uploaded CSAR file is first validated, then the onboarding process begins. The process does not progress if validation fails.

**Precondition**: The CSAR file complies with the [structure](#concepts) required for onboarding.

**Successful End Condition:** The App is onboarded, Helm charts and images are stored, and the App is ready for usage by the App LCM.

## Query Apps

**Description**: Query Apps that have been onboarded to App Manager.

Reads the current set of all onboarded Apps -- they can be filtered using available query parameters.

**Successful End Condition:** Returns a list of all Apps that are currently onboarded.

## Query an App

**Description**: Query for a specified App onboarded to App Manager.

Reads the full and current set of all onboarded Apps -- they can be filtered using available query parameters.

**Successful End Condition:** Returns the description and components of the App.

## Update an onboarded App

**Description**: Update one or many attributes of a specified onboarded App.

The values passed in the request body for the update are validated according to their respective attribute.

**Precondition**: The App status must be ``ONBOARDED`` before any attribute can be successfully updated.

**Successful End Condition:** The specified App is returned with the attribute(s) updated.

## Get list of Artifacts from an App

**Description**: Returns list of all artifacts associated with a specified onboarded App. The returned list contains complete information required to execute an App such as Helm charts and images.

The ``artifactId`` associated with each item in the list helps to determine certain artifacts to download.

**Successful End Condition:** Returns a list of artifacts ordered by their ``artifactId``.

## Get an Artifact from an App

**Description**:  Downloads the specified artifact based on the ``artifactId`` from a specific onboarded App. To determine the ``artifactId``, query for the list of artifacts from the same App.

**Successful End Condition:** The specified artifact is successfully downloaded.