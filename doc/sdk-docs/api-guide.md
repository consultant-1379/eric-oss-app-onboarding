# App Onboarding API Guide v1

This section covers the following API endpoints and role based access for onboarding a CSAR App Package with EIAP SDK:

- [Get All Onboarded Apps](#api-guide?chapter=get-all-onboarded-apps)
- [Onboard an App from a CSAR App Package File](#api-guide?chapter=onboard-an-app-from-a-csar-app-package-file)
- [Get an Onboarded App](#api-guide?chapter=get-an-onboarded-app)
- [Update an Onboarded App](#api-guide?chapter=update-an-onboarded-app)
- [Get List of App Artifacts](#api-guide?chapter=get-list-of-app-artifacts)
- [Get Artifact Information](#api-guide?chapter=get-artifact-information)
- [List of Endpoints](#api-guide?chapter=list-of-endpoints)
- [Role Based Access for App Onboarding](#api-guide?chapter=role-based-access-for-app-onboarding)

---

## Get All Onboarded Apps

**Description**:
Get or query a list of all apps onboarded to App Manager.

**Key Points**:

- By default, apps are sorted by their ``appId``.

**Path:**
`GET /onboarding/v1/apps`

**Request header**:
Content-Type: application/json.

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``.

| Response Code | Response Status | Description| Response Data Structure
| ------ | ------ | ------ | ------|
|**200**| OK | Successfully retrieved all apps | See Below |

**200 OK Response**:
```
[
  {
    "id": 1,
    "name": "eric-oss-5gcnr",
    "username": "Unknown",
    "version": "1.0.136-2",
    "size": "100MB",
    "vendor": "Ericsson",
    "type": "rApp",
    "onboardedDate": "2022-03-24T15:55:38.812+00:00",
    "permissions": [],
    "status": "ONBOARDED",
    "mode": "DISABLED",
    "artifacts": [
      {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "--",
        "status": "COMPLETED",
        "location": "/v2/proj-eric-oss-dev/eric-oss-anr5gassist/manifests/1.0.136-2"
      },
      {
        "id": 1,
        "name": "eric-oss-5gcnr",
        "type": "HELM",
        "version": "1.0.136-2",
        "status": "COMPLETED",
        "location": "/api/eric-oss-5gcnr_1.0.136-2/charts/eric-oss-5gcnr/1.0.136-2"
      }
    ],
    "permissions": [],
    "events": []
  }
]
```
---

## Onboard an App from a CSAR App Package File

**Description**:
Upload a CSAR app package and onboard it.

The package file must pass validation before it can be onboarded. Once validated, it is decompressed and its descriptor is parsed.

**Path**:
`POST /onboarding/v1/apps`

**Query Parameters**:
None at this stage.

**Request Body**:

| Name | Type | Description
| ------ | ------ | ------
| ``csarFile`` | String *$binary | The file to be uploaded for the installation of apps must be in .csar format and it must be all printable ASCII characters except these  \ / : * ? < > &#124;

**Request Header**:
Content-Type: multipart/form-data.

| Response Code | Response Status | Description | Response Data Structure                                                                                                                 
| ----- |------|------|-----------------------------------------------------------------------------------------------------------------------------------------|
|**202**| Accepted | Uploaded successfully | ><br/> The ``appId`` value returned in the response can be used as the ``appId`` parameter in GET /v1/apps/{``appId``}  <br/> See below |
|**400**| Bad Request | Returns error based on the validation | See below                                                                                                                               |

**202 Accepted Response**:
```
{
  "id": 2,
  "name": "test.csar",
  "username": "Unknown",
  "version": "1.1.1",
  "size": "100MB",
  "vendor": "Unknown",
  "type": "APP",
  "onboardedDate": "2022-03-28T15:41:48.551+00:00",
  "status": "UPLOADED",
  "mode": "DISABLED"
}
```

**400 Bad Request Response**:
```
{
  "errorCode": 123,
  "error": "Version already exists"
}
```
---

## Get an Onboarded App

**Description**:
Get or query a specified app onboarded to App Manager.

**Returns**:
An object containing the app description and its components.

**Path**:
``GET /onboarding/v1/apps/{appId}``

**Query Parameters**:

| Name | Type | Description                                       | Example
| ------ | ------ |---------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the app to be retrieved | /onboarding/v1/apps/1

**Request header**:
Content-Type: application/json.

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``.

| Response Code | Response Status | Description                                             | Response Data Structure
| ------ | ------ |---------------------------------------------------------| ------|
|**200**| OK | Successfully retrieved specified app                    | See below |
|**404**| Not Found | Returns error if app not found with specified ``appId`` | See below

**200 OK Response**:
```
{
  "id": 1,
  "name": "eric-oss-5gcnr",
  "username": "Unknown",
  "version": "1.0.136-2",
  "size": "100MB",
  "vendor": "Ericsson",
  "type": "rApp",
  "onboardedDate": "2022-03-24T15:55:38.812+00:00",
  "permissions": [],
  "status": "ONBOARDED",
  "mode": "DISABLED",
  "artifacts": [
    {
      "id": 2,
      "name": "docker.tar",
      "type": "IMAGE",
      "version": "--",
      "status": "COMPLETED",
      "location": "v2/proj-eric-oss-dev/eric-oss-anr5gassist/manifests/1.0.136-2"
    },
    {
      "id": 1,
      "name": "eric-oss-5gcnr",
      "type": "HELM",
      "version": "1.0.136-2",
      "status": "COMPLETED",
      "location": "/api/eric-oss-5gcnr_1.0.136-2/charts/eric-oss-5gcnr/1.0.136-2"
    }
  ],
  "permissions": [],
  "events": []
}
```

**404 Not Found Response**:
```
{
  "path": "/v1/apps/2",
  "method": "GET",
  "errorCode": 1500,
  "error": "Application not found, ID: 2 ",
  "timestamp": "2022-03-28T15:41:11.629+00:00",
  "status": "NOT_FOUND"
}
```
---

## Update an Onboarded App

**Description**:
Update one or more attributes of a fully onboarded app.

**Returns**:
An object containing the app description and its components with the updated attribute.

**Path**:
`PUT /onboarding/v1/apps/{appId}`

**Query Parameters**:

| Name | Type | Description                                     | Example
| ------ | ------ |-------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the app to be updated | /onboarding/v1/apps/1

**Request Body**:

| Name | Type | Description | Example
| ------ | ------ | ------ | ------
| ``mode `` | Raw JSON | The mode of an App indicates whether it is ENABLED or DISABLED for instantiation<br/><br/>``ENABLED`` - App instantiation can be performed<br/>``DISABLED`` - App instantiation cannot be performed<br/><br/>Set to ``DISABLED`` while/during onboarding an App |  ``{"mode": "ENABLED"}``

**Request header**:
Content-Type: application/json.

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``.

| Response Code | Response Status  | Description                                             | Response Data Structure
| ------ |------------------|---------------------------------------------------------| ------|
|**200**| OK               | Successfully updated specified app                      | See below |
|**400**| Bad Request      | Returns error based on the validation                   | See below |
|**404**| Not Found        | Returns error if app not found with specified ``appId`` | See below |

**200 OK Response**:
```
{
  "id": 1,
  "name": "eric-oss-5gcnr",
  "username": "Unknown",
  "version": "1.0.136-2",
  "size": "100MB",
  "vendor": "Ericsson",
  "type": "rApp",
  "onboardedDate": "2022-03-24T15:55:38.812+00:00",
  "permissions": [],
  "status": "ONBOARDED",
  "mode": "ENABLED",
  "artifacts": [
    {
      "id": 2,
      "name": "docker.tar",
      "type": "IMAGE",
      "version": "--",
      "status": "COMPLETED",
      "location": "v2/proj-eric-oss-dev/eric-oss-anr5gassist/manifests/1.0.136-2"
    },
    {
      "id": 1,
      "name": "eric-oss-5gcnr",
      "type": "HELM",
      "version": "1.0.136-2",
      "status": "COMPLETED",
      "location": "/api/eric-oss-5gcnr_1.0.136-2/charts/eric-oss-5gcnr/1.0.136-2"
    }
  ],
  "permissions": [],
  "events": []
}
```

**400 Bad Request Response**:
```
{
  "timestamp": "2022-03-28T15:33:39.396+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/v1/apps/1"
}
```

**404 Not Found Response**:
```
{
  "path": "/v1/apps/2",
  "method": "PUT",
  "errorCode": 1500,
  "error": "App not found, ID: 2 ",
  "timestamp": "2022-03-28T15:33:03.455+00:00",
  "status": "NOT_FOUND"
}
```
---

## Get List of App Artifacts

**Description**:
Display an app and list its artifacts.

**Key Points**:

- By default, artifacts are sorted by their ``artifactId``.
- ``errorResponse`` is used when the app contains no artifacts.

**Path**:
`GET /onboarding/v1/apps/{appId}/artifacts`

**Query Parameters**:

| Name | Type | Description                                       | Example
| ------ | ------ |---------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the app to be retrieved | /onboarding/v1/apps/2/artifacts

**Request header**:
Content-Type: application/json.

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``.

| Response Code | Response Status | Description                                                                                                                   | Response Data Structure
| ------ | ------ |-------------------------------------------------------------------------------------------------------------------------------| ------|
|**200**| OK | Successfully retrieved the artifacts in the app                                                                               | See below |
|**404**| Not Found | Returns error if app not found with specified ``appId``<br/>Returns error if artifact not found with specified ``artifactId`` | See below

**200 OK Response**:
```
[
    {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "--",
        "status": "COMPLETED",
        "location": "/v2/busybox/manifests/latest"
    },
    {
        "id": 1,
        "name": "eric-oss-app-onboarding",
        "type": "HELM",
        "version": "0.1.0-1",
        "status": "COMPLETED",
        "location": "/api/App-Onboarding-helloWorld_1.0.0/charts/eric-oss-app-onboarding/0.1.0-1"
    }
]
```

**404 Not Found Response**:
```
{
    "path": "/v1/apps/1/artifacts/3",
    "method": "GET",
    "errorCode": 2500,
    "error": "Could not find Artifact with id: 3  in Application with id:  1 ",
    "timestamp": "2022-03-28T16:11:18.880+00:00",
    "status": "NOT_FOUND"
}
```
---

## Get Artifact Information

**Description**:
Download an artifact from a specified app.

**Key Points**:

- The query also returns an object containing the app information and its artifacts.
- ``errorResponse`` is used when the pp contains no such artifact.

**Path**:
`GET /onboarding/v1/apps/{appId}/artifacts/{artifactId}`

**Query Parameters**:

| Name | Type | Description                                                                 | Example
| ------ | ------ |-----------------------------------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the app to be retrieved                           | /onboarding/v1/apps/2/artifacts/3
| ``artifactId`` | Integer | The numeric identifier of the artifact of the specified app to be retrieved | /onboarding/v1/apps/2/artifacts/3

**Request header**:
Content-Type: application/json.

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``.

| Response Code | Response Status | Description                                                                                                                   | Response Data Structure
| ------ | ------ |-------------------------------------------------------------------------------------------------------------------------------| ------|
|**200**| OK | Successfully retrieved the artifacts in the app                                                                               | See below
|**404**| Not Found | Returns error if app not found with specified ``appId``<br/>Returns error if artifact not found with specified ``artifactId`` | See below

**200 OK Response**:
```
{
    "id": 2,
    "name": "docker.tar",
    "type": "IMAGE",
    "version": "--",
    "status": "COMPLETED",
    "location": "v2/proj-eric-oss-dev/eric-oss-anr5gassist/manifests/1.0.136-2"
}
```

**404 Not Found Response**:
```
{
    "path": "/v1/apps/1/artifacts/3",
    "method": "GET",
    "errorCode": 2500,
    "error": "Could not find Artifact with id: 3  in Application with id:  1 ",
    "timestamp": "2022-03-28T16:11:18.880+00:00",
    "status": "NOT_FOUND"
}
```
---

## List of Endpoints

The table summarizes the API Gateway route definitions.

**Note**: Each endpoint has `/v1/routes` as the base-path.

| Path           | HTTP method | Description                                           | Response Code | Response Status       |
| -------------- | ----------- |-------------------------------------------------------| ------------- | --------------------- |
| v1/apps | GET | Displays all onboarded apps.                          | 200 | OK |
| v1/apps | POST | Onboards a new app to App Manager.                    | 202, 400 | Accepted, Bad Request |
| v1/apps/{``appId``} | GET | Displays an onboarded app.                            | 200, 404 | OK, Not Found |
| v1/apps/{``appId``} | PUT | Updates an onboarded app.                             | 200, 400, 404 | OK, Bad Request, Not Found |
| v1/apps/{``appId``}/artifacts | GET | Displays a list of all artifacts from an app.         | 200, 404 | OK, Not Found |
| v1/apps/{``appId``}/artifacts/{``artifactId``} | GET | Downloads an artifact from the list of app artifacts. | 200, 404 | OK, Not Found |

## Error Codes

| Status Code | Text | Description|
|---|-------------|------------|
| 400 | 	Bad Request | The request was invalid. |
| 404 | 	Not Found  | Artifact not found with specified ID. |

---

## Role-based Access for App Onboarding

**Description**: App Manager access is based on roles assigned to the user.

An App Manager user can be assigned one of the following predifined roles:
- Role AppMgrAdmin
- Role AppMgrOperator

List of roles within App Manager and their corresponding action privileges.

|Resource                  | Action           | Role AppMgrAdmin  | Role AppMgrOperator  |
| ------------------------ | ---------------- | ----------------- | -------------------- |
|AppOnboarding Apps        | Create           | Yes               | No                   |
|                          | View             | Yes               | Yes                  |
|                          | Edit             | Yes               | No                   |
|AppOnboarding Artifacts   | View             | Yes               | Yes                  |
|AppLCM App Instances      | Create           | Yes               | Yes                  |
|                          | View             | Yes               | Yes                  |
|                          | Edit             | Yes               | Yes                  |
|AppLCM Artifact Instances | View             | Yes               | Yes                  |