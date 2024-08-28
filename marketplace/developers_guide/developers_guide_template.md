# App Onboarding Developers Guide

This document provides guidelines for how to use App Onboarding from an application developer’s point of view.

It gives a brief description of its main features, and the following interfaces.

- [V1 Interfaces](#V1-Interfaces)
- [V2 Interfaces](#V2-Interfaces)


## V1 Interfaces

---
This section covers the following developer-related topics for App Onboarding V1:

- [Get All Onboarded Apps](#Get-All-Onboarded-Apps)
- [Onboard an App from a CSAR App Package File](#Onboard-an-App-from-a-CSAR-App-Package-File)
- [Get an Onboarded App](#Get-an-Onboarded-App)
- [Update an Onboarded App](#Update-an-Onboarded-App)
- [Get List of App Artifacts](#Get-List-of-App-Artifacts)
- [Get Artifact Information](#Get-Artifact-Information)
- [List Endpoints](#List-of-V1-Endpoints)

### Get All Onboarded Apps

**Description**:
Get or query a list of all Apps onboarded to App Manager.

**Key Points**:

- By default, Apps are sorted by their ``appId``.
- **Filters** and **Matchers** such as ``appName``, ``appId``, ``appVersion`` and ``vendor`` can be used to condense the resulting list of onboarded Apps.
- A custom range of Apps from the resulting list can be selected by setting the ``offset`` (starting point) and ``limit`` (number of list items).

**Path:**
`GET /onboarding/v1/apps`

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code | Description                     | Response Data Structure
| ------ |---------------------------------| ------
|**200 OK**| Successfully retrieved all Apps | See below

**200 OK Response**:

```json
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
    "status": "ONBOARDED",
    "mode": "DISABLED",
    "artifacts": [
      {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "1.0.136-2",
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
    "events": [],
    "roles": []
  }
]
```

### Onboard an App from a CSAR App Package File

**Description**:
Upload a CSAR App package and onboard it.

The package file must pass validation before it can be onboarded. Once validated, it is decompressed and its descriptor is parsed.

**Path**:
`POST /onboarding/v1/apps`

**Request Body**:

| Name     | Type | Description
|----------| ------ |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
| ``file`` | String *$binary | The file to be uploaded for the instantiation of Apps must be in .csar format and it must be all printable ASCII characters except these  \ / : * ? < > &#124;

**Request Header**:
Content-Type: multipart/form-data

| Response Code | Description | Response Data Structure
| ------ | ------ | ------
|**202 Accepted**| Uploaded successfully |The ``appId`` value returned in the response can be used as the ``appId`` parameter in GET /v1/apps/{``appId``}. <br>See below
|**400 Bad Request**| Returns error based on the validation |See below

**202 Accepted Response**:

```json
{
  "id": 2,
  "name": "test.csar",
  "username": "Unknown",
  "version": "1.1.1",
  "size": "100MB",
  "vendor": "Unknown",
  "type": "APP",
  "onboardedDate": "2022-03-28T15:41:48.551+00:00",
  "permissions": [],
  "status": "UPLOADED",
  "mode": "DISABLED"
}
```

**400 Bad Request Response**:

```json
{
  "path": "/v1/apps",
  "method": "POST",
  "errorCode": 123,
  "error": "File extension is invalid, please upload a file with .csar extension.",
  "timestamp": "2023-03-22T10:51:57.235+00:00",
  "status": "BAD_REQUEST"
}
```

### Get an Onboarded App

**Description**:
Get or query a specified App onboarded to App Manager.

**Returns**:
An object containing the App description and its components.

**Path**:
``GET /onboarding/v1/apps/{appId}``

**Path Parameters**:

| Name | Type | Description | Example
| ------ | ------ | ------ | ------
| ``appId`` | Integer | The numeric identifier of the App to be retrieved | /onboarding/v1/apps/1

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code | Description                                             | Response Data Structure
| ------ |---------------------------------------------------------| ------
|**200 OK**| Successfully retrieved specified App                    | See below
|**404 Not Found**| Returns error if App not found with specified ``appId`` | See below

**200 OK Response**:

```json
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
    "status": "ONBOARDED",
    "mode": "DISABLED",
    "artifacts": [
      {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "1.0.136-2",
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
    "events": [],
    "roles": []
  }
]
```

**404 Not Found Response**:

```json
{
  "path": "/v1/apps/2",
  "method": "GET",
  "errorCode": 1500,
  "error": "Application not found, ID: 2 ",
  "timestamp": "2022-03-28T15:41:11.629+00:00",
  "status": "NOT_FOUND"
}
```

### Update an Onboarded App

**Description**:
Update the mode attribute of a successfully onboarded App

**Returns**:
An object containing the App description and its components with the updated attribute.

**Path**:
`PUT /onboarding/v1/apps/{appId}`

**Path Parameter**:

| Name | Type | Description                                     | Example
| ------ | ------ |-------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the App to be updated | /onboarding/v1/apps/1

**Request Body**:

| Name | Type | Description                                                                                                                                                                                                                                                     | Example
| ------ | ------ |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| ------
| ``mode`` | Raw JSON | The mode of an App indicates whether it is ENABLED or DISABLED for instantiation</br></br>``ENABLED`` - App instantiation can be performed</br>``DISABLED`` - App instantiation cannot be performed</br></br>Set to ``DISABLED`` while/during onboarding an App |  ``{"mode": "ENABLED"}``
**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the
Content-Type response header: ``application/json``

| Response Code | Description                                             | Response Data Structure
| ------ |---------------------------------------------------------| ------
|**200 OK**| Successfully updated specified App                      | See below
|**400 Bad Request**| Returns error based on the validation                   | See below
|**404 Not Found**| Returns error if App not found with specified ``appId`` | See below

**200 OK Response**:

```json
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
    "status": "ONBOARDED",
    "mode": "ENABLED",
    "artifacts": [
      {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "1.0.136-2",
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
    "events": [],
    "roles": []
  }
]
```

**400 Bad Request Response**:

```json
{
  "timestamp": "2022-03-28T15:33:39.396+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/v1/apps/1"
}
```

**404 Not Found Response**:

```json
{
  "path": "/v1/apps/2",
  "method": "PUT",
  "errorCode": 1500,
  "error": "App not found, ID: 2 ",
  "timestamp": "2022-03-28T15:33:03.455+00:00",
  "status": "NOT_FOUND"
}
```

### Get List of App Artifacts

**Description**:
Display an App and list its artifacts.

**Key Points**:

- By default, artifacts are sorted by their ``artifactId`` .
- ``errorResponse`` is used when the App contains no artifacts.

**Path**:
`GET /onboarding/v1/apps/{appId}/artifacts`

**Path Parameters**:

| Name | Type | Description                                       | Example
| ------ | ------ |---------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the App to be retrieved | /onboarding/v1/apps/2/artifacts

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code | Description                                                                                                                   | Response Data Structure
| ------ |-------------------------------------------------------------------------------------------------------------------------------| ------
|**200 OK**| Successfully retrieved the artifacts in the App                                                                               | See below
|**404 Not Found**| Returns error if App not found with specified ``appId`` | See below

**200 OK Response**:

```json
[
    {
        "id": 2,
        "name": "docker.tar",
        "type": "IMAGE",
        "version": "1.0.136-2",
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
]
```

**404 Not Found Response**:

```json
{
    "path": "/v1/apps/100/artifacts",
    "method": "GET",
    "errorCode": 1500,
    "error": "Application not found, ID: 100",
    "timestamp": "2022-03-28T16:11:18.880+00:00",
    "status": "NOT_FOUND"
}
```

### Get Artifact Information

**Description**:
Get artifact details from a specified App.

**Key Points**:

- The query returns an object containing the App information and its artifacts.
- ``errorResponse`` is used when the App contains no such artifact.

**Path**:
`GET /onboarding/v1/apps/{appId}/artifacts/{artifactId}`

**Path Parameters**:

| Name | Type | Description                                                                 | Example
| ------ | ------ |-----------------------------------------------------------------------------| ------
| ``appId`` | Integer | The numeric identifier of the App to be retrieved                           | /onboarding/v1/apps/**2**/artifacts/3
| ``artifactId`` | Integer | The numeric identifier of the artifact of the specified App to be retrieved | /onboarding/v1/apps/2/artifacts/**3**

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the
``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code | Description                                                                                                                   | Response Data Structure
| ------ |-------------------------------------------------------------------------------------------------------------------------------| ------
|**200 OK**| Successfully retrieved the artifact in the App                                                                                | See below
|**404 Not Found**| Returns error if App not found with specified ``appId``</br>Returns error if artifact not found with specified ``artifactId`` | See below

**200 OK Response**:

```json
{
    "id": 2,
    "name": "docker.tar",
    "type": "IMAGE",
    "version": "1.0.136-2",
    "status": "COMPLETED",
    "location": "v2/proj-eric-oss-dev/eric-oss-anr5gassist/manifests/1.0.136-2"
}
```

**404 Not Found Response**:

```json
{
    "path": "/v1/apps/1/artifacts/3",
    "method": "GET",
    "errorCode": 2500,
    "error": "Could not find Artifact with id: 3  in Application with id:  1 ",
    "timestamp": "2022-03-28T16:11:18.880+00:00",
    "status": "NOT_FOUND"
}
```

### List of V1 Endpoints

The table summarizes the API Gateway route definitions.

**Note**: Each endpoint has `/v1/routes` as the base-path.

| Path           | HTTP method | Description                                      | Response Code | Response Status       |
| -------------- | ----------- |--------------------------------------------------| ------------- | --------------------- |
| v1/apps | GET | Displays all onboarded Apps.                     | 200 | OK |
| v1/apps | POST | Onboards a new App to App Manager.               | 202, 400 | Accepted, Bad Request |
| v1/apps/{``appId``} | GET | Displays an onboarded App.                       | 200, 404 | OK, Not Found |
| v1/apps/{``appId``} | PUT | Updates an onboarded App.                        | 200, 400, 404 | OK, Bad Request, Not Found |
| v1/apps/{``appId``}/artifacts | GET | Displays a list of all artifacts from an App.    | 200, 404 | OK, Not Found |
| v1/apps/{``appId``}/artifacts/{``artifactId``} | GET | Displays the details of an artifact from an App. | 200, 404 | OK, Not Found |

<br>

## V2 Interfaces

---
This section covers the following developer-related topics for App Onboarding V2:

- [Onboard a CSAR App Package](#Onboard-a-CSAR-App-Package)
- [Get All Onboarding Jobs](#Get-All-Onboarding-Jobs)
- [Get an Onboarding Job](#Get-an-Onboarding-Job)
- [Delete an Onboarding Job](#Delete-an-Onboarding-Job)
- [List Endpoints](#List-of-V2-Endpoints)

### Onboard a CSAR App Package

**Description**:
Onboards an App Package in the system. The provided App package is uploaded, and an onboarding-job is created
to handle the onboarding process.

A successful response to this request indicates that the request was accepted, the package was uploaded successfully and is now being processed.
While the onboarding-job is executed, the artifacts in the package are extracted and stored internally, and a new App representing
the CSAR package content is created in the system.

Users can `GET` the onboarding-job using the `id` or link provided in the response, and check the execution status until the onboarding has completed successfully.
Once the onboarding-job is created successfully, an `appId` is provided for lifecycle management of the App via App Manager Lifecycle Management (LCM).

**Path**:
`POST /onboarding/v2/app-packages`

**Request Body**:

| Name     | Type              | Description                                         |
|----------|-------------------|-----------------------------------------------------|
| ``file`` | String *$binary   | The file to be uploaded must be in .csar format     |

**Request Header**:
Content-Type: multipart/form-data

| Response Code                 | Description                           | Response Data Structure                                                                                                                   |
|-------------------------------|---------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **202 Accepted**              | Uploaded successfully                 | The ``jobId`` value returned in the response can be used as the ``jobId`` parameter in GET /v2/onboarding-jobs/{``jobId``}. <br>See below |
| **400 Bad Request**           | Returns error based on the validation | See below                                                                                                                                 |

**202 Accepted Response**:

```json
{
  "fileName": "eric-oss-hello-world-app.csar",
  "onboardingJob": {
    "id": "9cc1047a-5aae-4630-893a-1536392cbd2b",
    "href": "app-onboarding/v2/onboarding-jobs/9cc1047a-5aae-4630-893a-1536392cbd2b"
  }
}
```

**400 Bad Request Response**:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "File type is invalid, helloworldBadRequest.yaml. Please onboard a valid csar archive file."
}
```

### Get All Onboarding Jobs

**Description**:
Gets all onboarding-jobs created in App Manager or a filtered list of onboarding-jobs using the optional query parameters.  
Note that only a single property query parameter is allowed in the request. Query parameters are handled 
in a case sensitive manner, and unrecognised parameters will be ignored.

**Key Points**:

- By default, Onboarding Jobs are sorted by their ``jobId``.
- **Filters** and **Matchers** such as ``id``, ``fileName``, ``vendor``, ``packageVersion``, ``type``, ``packageSize``, ``status`` and ``appId`` can be used to condense the resulting list of Onboarding Jobs.
- A custom range of Onboarding Jobs from the resulting list can be selected by setting the ``offset`` (starting point) and ``limit`` (number of list items).

**Path:**
`GET /onboarding/v2/onboarding-jobs`

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code       | Description                                | Response Data Structure   | 
|---------------------|--------------------------------------------|---------------------------| 
| **200 OK**          | Successfully retrieved all Onboarding Jobs | See below                 | 
| **400 Bad Request** | Returns error based on an invalid request  | See below                 |

**200 OK Response**:

```json
{
  "items": [
    {
      "id": "9cc1047a-5aae-4630-893a-1536392cbd2b",
      "fileName": "eric-oss-hello-world-app.csar",
      "packageVersion": "1.1.1",
      "packageSize": "100MiB",
      "vendor": "Ericsson",
      "type": "rApp",
      "onboardStartedAt": "2023-12-20T12:00:06.996762Z",
      "status": "ONBOARDED",
      "onboardEndedAt": "2023-12-20T12:00:54.798965Z",
      "events": [
        {
          "type": "INFO",
          "title": "Stored 1 out of 3 artifacts",
          "detail": "Uploaded eric-oss-hello-world-app",
          "occurredAt": "2023-12-20T12:00:47.937804Z"
        },
        {
          "type": "INFO",
          "title": "Stored 2 out of 3 artifacts",
          "detail": "Uploaded docker.tar",
          "occurredAt": "2023-12-20T12:00:48.063780Z"
        },
        {
          "type": "INFO",
          "title": "Stored 3 out of 3 artifacts",
          "detail": "Uploaded ASD.yaml",
          "occurredAt": "2023-12-20T12:00:48.110092Z"
        }
      ],
      "self": {
        "href": "app-onboarding/v2/onboarding-jobs/9cc1047a-5aae-4630-893a-1536392cbd2b"
      },
      "app": {
        "id": "26471a81-1de4-4ad9-9724-326eefd22230",
        "href": "app-lifecycle-management/v3/apps/26471a81-1de4-4ad9-9724-326eefd22230"
      }
    }
  ]
}
```

**400 Bad Request Response**:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "Query Parameter ID should be a valid number"
}
```

### Get an Onboarded Job

**Description**:
Returns the onboarding-job for the given onboarding-job `id` value.

**Returns**:
An object containing the Onboarding Job description.

**Path**:
``GET /onboarding/v2/onboarding-jobs/{jobId}``

**Path Parameters**:

| Name      | Type            | Description                                            | Example                                                             | 
|-----------|-----------------|--------------------------------------------------------|---------------------------------------------------------------------| 
| ``jobId`` | string($uuid)   | The identifier of the Onboarding Job to be retrieved   | /onboarding/v2/onboarding-jobs/9cc1047a-5aae-4630-893a-1536392cbd2b | 

**Request header**:
Content-Type: application/json

**Responses**:
This API call produces media type according to the ``Accept`` request header.

The media type is conveyed by the Content-Type response header: ``application/json``

| Response Code       | Description                                             | Response Data Structure |
|---------------------|---------------------------------------------------------|-------------------------|
| **200 OK**          | Successfully retrieved specified Onboarding Job         | See below               |
| **400 Bad Request** | Returns error based on an invalid request               | See below               |
| **404 Not Found**   | Returns error if App not found with specified ``jobId`` | See below               |

**200 OK Response**:

```json
{
  "id": "9cc1047a-5aae-4630-893a-1536392cbd2b",
  "fileName": "eric-oss-hello-world-app.csar",
  "packageVersion": "1.1.1",
  "packageSize": "100MiB",
  "vendor": "Ericsson",
  "type": "rApp",
  "onboardStartedAt": "2023-12-20T12:00:06.996762Z",
  "status": "ONBOARDED",
  "onboardEndedAt": "2023-12-20T12:00:54.798965Z",
  "events": [
    {
      "type": "INFO",
      "title": "Stored 1 out of 3 artifacts",
      "detail": "Uploaded eric-oss-hello-world-app",
      "occurredAt": "2023-12-20T12:00:47.937804Z"
    },
    {
      "type": "INFO",
      "title": "Stored 2 out of 3 artifacts",
      "detail": "Uploaded docker.tar",
      "occurredAt": "2023-12-20T12:00:48.063780Z"
    },
    {
      "type": "INFO",
      "title": "Stored 3 out of 3 artifacts",
      "detail": "Uploaded ASD.yaml",
      "occurredAt": "2023-12-20T12:00:48.110092Z"
    }
  ],
  "self": {
    "href": "app-onboarding/v2/onboarding-jobs/9cc1047a-5aae-4630-893a-1536392cbd2b"
  },
  "app": {
    "id": "26471a81-1de4-4ad9-9724-326eefd22230",
    "href": "app-lifecycle-management/v3/apps/26471a81-1de4-4ad9-9724-326eefd22230"
  }
}
```

**400 Bad Request Response**:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "An error occurred when processing request"
}
```

**404 Not Found Response**:

```json
{
  "status": 404,
  "title": "Not Found",
  "detail": "Onboarding-job with ID 9cc1047a-5aae-4630-893a-1536392cbd2b was not found"
}
```

### Delete an Onboarding Job

**Description**:
Deletes an onboarding-job for the given onboarding-job `id`. The job and its summary data are deleted from the system.

**Returns**:
204 No content

**Path**:
``DELETE /onboarding/v2/onboarding-jobs/{jobId}``

**Path Parameters**:

| Name      | Type            | Description                                        | Example                                                             | 
|-----------|-----------------|----------------------------------------------------|---------------------------------------------------------------------| 
| ``jobId`` | string($uuid)   | The identifier of the Onboarding Job to be deleted | /onboarding/v2/onboarding-jobs/9cc1047a-5aae-4630-893a-1536392cbd2b | 

**Request header**:
Content-Type: application/json

**Responses**:

| Response Code       | Description                                             | Response Data Structure |
|---------------------|---------------------------------------------------------|-------------------------|
| **204 No Content**  | Successfully deleted specified Onboarding Job           | No Content              |
| **400 Bad Request** | Returns error based on an invalid request               | See below               |
| **404 Not Found**   | Returns error if App not found with specified ``jobId`` | See below               |


**400 Bad Request Response**:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "An error occurred when processing request"
}
```

**404 Not Found Response**:

```json
{
  "status": 404,
  "title": "Not Found",
  "detail": "Onboarding-job with ID 9cc1047a-5aae-4630-893a-1536392cbd2b was not found"
}
```

### List of V2 Endpoints

The table summarizes the API Gateway route definitions.

**Note**: Each endpoint has `/v2/routes` as the base-path.

| Path                           | HTTP method | Description                  | Response Code | Response Status                    |
|--------------------------------|-------------|------------------------------|---------------|------------------------------------|
| v2/app-packages                | POST        | Onboards a new App           | 202, 400      | Accepted, Bad Request              |
| v2/onboarding-jobs             | GET         | Displays all Onboarding Jobs | 200, 400      | OK, Bad Request                    |
| v2/onboarding-jobs/{``jobId``} | GET         | Displays an Onboarding Job   | 200, 400, 404 | OK, Bad Request, Not Found         |
| v2/onboarding-jobs/{``jobId``} | DELETE      | Deletes an Onboarding Job    | 204, 400, 404 | No Content, Bad Request, Not Found |

