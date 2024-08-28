# Create a CSAR App Package

This section describes how to create a CSAR App Package.

## Steps

### Step 1: Create a folder for the CSAR skeleton files
```
mkdir -p csar
```

### Step 2: Create the CSAR directories structure
```
cd csar
mkdir -p Definitions Metadata OtherDefinitions/ASD
```

### Step 3: Create the CSAR yaml files
Create the AppDescriptor.yaml and Tosca.meta files.
```
touch Definitions/AppDescriptor.yaml Metadata/tosca.meta
```
For further information about those files, see [Concepts](#concepts).

### Step 4: Create a folder for the CSAR App Package
```
cd ..
mkdir -p <APP PACKAGE NAME>
```

### Step 5: Copy the CSAR skeleton files to the App Package directory
```
cp -r csar/* <APP PACKAGE NAME>/
```

### Step 6: Create a Helm Chart archive

```
cd <PATH TO EXISTING CHARTS>/<CHART NAME>
helm package .
```

### Step 7: Copy the archive file for the Helm Chart to the folder ***ASD***
Move the .tgz file created in step 6 to the ***OtherDefinitions/ASD*** directory.
```
mv <CHART NAME>-<CHART VERSION>.tgz ../../<APP PACKAGE NAME>/OtherDefinitions/ASD
cd ../../
```

### Step 8: Create CSAR App Package command
Run the below command locally to create csar app package using the eric-oss-app-package-tool.

**Example:**
```
docker run --init --rm \
--volume /tmp/csar/:/tmp/csar/ \
--volume "$HOME"/.docker:/root/.docker \
--volume /var/run/docker.sock:/var/run/docker.sock \
--workdir /target \
--volume <ABSOLUTE PATH TO THE PROJECT>/<APP PACKAGE NAME>:/target
armdocker.rnd.ericsson.se/proj-eric-oss-dev-test/releases/eric-oss-app-package-tool:latest \
generate --tosca /target/Metadata/Tosca.meta \
--name <APP PACKAGE NAME> \
--helm3 \
--output /tmp/csar
```


### Step 9 (Optional): Add the required Images for the CSAR App Package
Docker Images can be placed in the ***Images/*** directory as docker.tar.
By default the package-tool will pull the Images referenced in the Chart and save them to a docker.tar in the Images directory.
They can be passed into the tool using an additional flag ***--images*** <path to docker.tar>.

Passing the Docker Images will prevent the tool from pulling them from the Docker repo.

```
docker save <DOCKER IMAGE ID> -o <PATH>/docker.tar
mkdir <APP PACKAGE NAME>/OtherDefinitions/ASD/Images
cp <PATH>/docker.tar <APP PACKAGE NAME>/OtherDefinitions/ASD/Images
```

# Reference

To find out more about Dockerfile, see [docker docs](https://docs.docker.com)