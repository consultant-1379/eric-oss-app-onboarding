# Java Spring Boot Chassis

The Spring Boot Microservice Chassis is a typical spring boot application with a few additions to enable the service to be built, tested, containerized and deployed on a Kubernetes cluster.
The Chassis is available as a Gerrit repository that can be cloned and duplicated to create new microservice.
While there may be a need to create multiple chassis templates based on the choice of build tool, application frameworks and dependencies the current implementation is a Java and Spring Boot Maven project.

## Contact Information
#### Team Members
##### Chassis
Team Quaranteam are currently the acting development team working on the Microservice Chassis.
For support please contact Thomas Kelly <a href="mailto:thomas.kelly@ericsson.com"> thomas.kelly@ericsson.com</a>

##### CI Pipeline
The CI Pipeline aspect of the Microservice Chassis is now owned, developed and maintained by [Team Hummingbirds](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ACE/Hummingbirds+Home) in the DE (Development Environment) department of PDU OSS.

#### Email
Guardians for this project can be reached at Team Quaranteam <PDLDUNQUAR@pdl.internal.ericsson.com> or through the <a href="https://teams.microsoft.com/l/channel/19%3a9bc0c88ae51e4c77ae35092123673db8%40thread.skype/Developer%2520Experience?groupId=f7576b61-67d8-4483-afea-3f6e754486ed&tenantId=92e84ceb-fbfd-47ab-be52-080c6b87953f">ADP Developer Experience MS Teams Channel</a>

## Maven Dependencies
The chassis has the following Maven dependencies:
  - Spring Boot Start Parent version 2.5.2.
  - Spring Boot Starter Web.
  - Spring Boot Actuator.
  - Spring Cloud Sleuth.
  - Spring Boot Started Test.
  - JaCoCo Code Coverage Plugin.
  - Sonar Maven Plugin.
  - Spotify Dockerfile Maven Plugin.
  - Common Logging utility for logback created by Vortex team.
  - Properties for spring cloud version and java are as follows.
  ```
      <version.spring-cloud>2020.0.3</version.spring-cloud>
  ```

## Build related artifacts
The main build tool is BOB provided by ADP. For convenience, maven wrapper is provided to allow the developer to build in an isolated workstation that does not have access to ADP.
  - [ruleset2.0.yaml](ruleset2.0.yaml) - for more details on BOB please click here [BOB].
  - [precoderview.Jenkinsfile](precodereview.Jenkinsfile) - For pre code review Jenkins pipeline that runs when patch set is pushed.
  - [publish.Jenkinsfile](publish.Jenkinsfile) - For publish Jenkins pipeline that runs after patch set is merged to master.
  - [.bob.env](.bob.env)

If the developer wishes to manually build the application in the local workstation, the command ``` bob release ``` can be used once BOB is configured in the workstation.

Furthermore, stub jar files are necessary to allow contract tests to run. The stub jars are stored in JFrog (Artifactory).
To allow the contract test to access and retrieve the stub jars, the .bob.env file must be configured as follows.
  ```
      SELI_ARTIFACTORY_REPO_USER=<LAN user id>
      SELI_ARTIFACTORY_REPO_PASS=<JFrog encripted LAN PWD or API key>
      HOME=<path containing .m2, e.g. /c/Users/<user>/>
  ```
To retrieve an encrypted LAN password or API key, login to [JFrog] and select "Edit Profile". For info in setting the .bob.env file see [Bob 2.0 User Guide].

## Containerization and Deployment to Kubernetes cluster.
Following artifacts contains information related to building a container and enabling deployment to a Kubernetes cluster:
- [charts](charts/) folder - used by BOB to lint, package and upload helm chart to helm repository.
  -  Once the project is built in the local workstation using the ```bob release``` command, a packaged helm chart is available in the folder ```.bob/eric-oss-app-onboarding-internal/``` folder. This chart can be manually installed in Kubernetes using ```helm install``` command. [P.S. required only for Manual deployment from local workstation]
- [Dockerfile](Dockerfile) - used by Spotify dockerfile maven plugin to build docker image.
  - The base image for the chassis application is ```sles-jdk8``` available in ```armdocker.rnd.ericsson.se```.

## Source
The [src](src/) folder of the java project contains a core spring boot application, a controller for health check and an interceptor for helping with logging details like user name. The folder also contains corresponding java unit tests.

```
src
├── main
│   ├── java
│   │   ├── com
│   │   │ └── ericsson
│   │   │     └── de
│   │   │         ├── client
│   │   │         │   └── example
│   │   │         │       └── SampleRestClient.java
│   │   │         ├── controller
│   │   │         │   ├── package-info.java
│   │   │         │   ├── example
│   │   │         │   │   ├── SampleApiControllerImpl.java
│   │   │         │   │   └── package-info.java
│   │   │         │   └── health
│   │   │         │       ├── HealthCheck.java
│   │   │         │       └── package-info.java
│   │   │         ├── CoreApplication.java
│   │   │         └── package-info.java
│   │   └── META-INF
│   │       └── MANIFEST.MF
│   └── resources
│       ├── jmx
│       │   ├── jmxremote.access
│       │   └── jmxremote.password
│       ├── v1
│       │   ├── index.html
│       │   └── microservice-chassis-openapi.yaml
│       ├── application.yaml
│       ├── logback-json.xml
│       └── bootstrap.yml
└── test
    └── java
        └── com
            └── ericsson
                └── de
                    ├── api
                    │   └── contract
                    │       ├── package-info.java
                    │       └── SampleApiBase.java
                    ├── business
                    │       └── package-info.java
                    ├── client
                    │   └── example
                    │       └── SampleRestClientTest.java
                    ├── controller
                    │   ├── example
                    │   │   ├── SampleApiControllerTest.java
                    │   │   └── package-info.java
                    │   └── health
                    │       ├── HealthCheckTest.java
                    │       └── package-info.java
                    ├── repository
                    │       └── package-info.java
                    ├── CoreApplicationTest.java
                    └── package-info.java
```


## Setting up CI Pipeline
-  Docker Registry is used to store and pull Docker images. At Ericsson official chart repository is maintained at the org-level JFrog Artifactory. Follow the link to set up a [Docker registry].
-  Helm repo is a location where packaged charts can be stored and shared. The official chart repository is maintained at the org-level JFrog Artifactory. Follow the link to set up a [Helm Repo].
-  Follow instructions at [Jenkins Pipeline setup] to use out-of-box Jenkinsfiles which comes along with eric-oss-app-onboarding.
-  Jenkins Setup involves master and agent machines. If there is not any Jenkins master setup, follow instructions at [Jenkins Master] - 2.89.2 (FEM Jenkins).
- Request a node from the GIC (Note: RHEL 7 GridEngine Nodes have been successfully tested).
 [Request Node] (https://estart.internal.ericsson.com/)
-  To setup [Jenkins agent] to for Jenkins, jobs execution follow the instructions at Jenkins Agent Setup.
-  Follow instructions at [Customize BOB] Ruleset Based on Your Project Settings to update ruleset files to suit to your project needs.

   [SLF4J] (https://logging.apache.org/log4j/2.x/log4j-slf4j-impl/index.html)
   [Gerrit Repos] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Design+and+Development+Environment)
   [BOB] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Adopting+BOB+Into+the+MVP+Project)
   [Bob 2.0 User Guide] (https://gerrit-gamma.gic.ericsson.se/plugins/gitiles/adp-cicd/bob/+/refs/heads/master/USER_GUIDE_2.0.md)
   [Docker registry] (https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=ACD&title=How+to+create+new+docker+repository+in+ARM+artifactory)
   [Helm repo] (https://confluence.lmera.ericsson.se/display/ACD/How+to+setup+Helm+repositories+for+ADP+e2e+CICD)
   [Jenkins Master] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsMaster-2.89.2(FEMJenkins))
   [Jenkins agent] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-Prerequisites)
   [Jenkins Pipeline setup] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-JenkinsPipelinesetup)
   [EO Common Logging] (https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ESO/EO+Common+Logging+Library)
   [JFrog] (https://arm.seli.gic.ericsson.se)

## Using the Helm Repo API Token
The Helm Repo API Token is usually set using credentials on a given Jenkins FEM.
If the project you are developing is part of IDUN/Aeonic this will be pre-configured for you.
However, if you are developing an independent project please refer to the 'Helm Repo' section:
[App Onboarding handles the onboarding of an O-RAN App Package, unpacking and distributing the artefacts within the app package. CI Pipeline Guide]https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/PCNCG/Microservice+Chassis+CI+Pipeline+Start+Guide#MicroserviceChassisCIPipelineStartGuide-HelmRepo

Once the Helm Repo API Token is made available via the Jenkins job credentials the precodereview and publish Jenkins jobs will accept the credentials (ex. HELM_SELI_REPO_API_TOKEN' or 'HELM_SERO_REPO_API_TOKEN) and create a variable HELM_REPO_API_TOKEN which is then used by the other files.

Credentials refers to a user or a functional user. This user may have access to multiple Helm repos.
In the event where you want to change to a different Helm repo, that requires a different access rights, you will need to update the set credentials.

## Artifactory Set-up Explanation
The App Onboarding handles the onboarding of an O-RAN App Package, unpacking and distributing the artefacts within the app package. Artifactory repos (dev, ci-internal and drop) are set up following the ADP principles: https://confluence.lmera.ericsson.se/pages/viewpage.action?spaceKey=AA&title=2+Repositories

The commands: "bob init-dev build image package" will ensure that you are pushing a Docker image to:
[Docker registry - Dev] https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-dev/

The Precodereview Jenkins job pushes a Docker image to:
[Docker registry - CI Internal] https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-ci-internal/

This is intended behaviour which mimics the behavior of the Publish Jenkins job.
This job presents what will happen when the real microservice image is being pushed to the drop repository.
Furthermore, the 'Helm Install' stage needs a Docker image which has been previously uploaded to a remote repository, hence why making a push to the CI Internal is necessary.

The Publish job also pushes to the CI-Internal repository, however the Publish stage promotes the Docker image and Helm chart to the drop repo:
[Docker registry - Drop] https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-eric-oss-drop/

Similarly, the Helm chart is being pushed to three separate repositories:
[Helm registry - Dev] https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-dev-helm/

The Precodereview Jenkins job pushes the Helm chart to:
[Helm registry - CI Internal] https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-ci-internal-helm/

This is intended behaviour which mimics the behavior of the Publish Jenkins job.
This job presents what will happen when the real Helm chart is being pushed to the drop repository.
The Publish Jenkins job pushes the Helm chart to:
[Helm registry - Drop] https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/
