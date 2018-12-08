# WebLogic Sample Domain Home in Docker Image

The sample scripts demonstrate the creation of a WebLogic domain home in docker image. The scripts also generate the domain YAML file, which can then be used to start the Kubernetes artifacts of the corresponding domain. Optionally, the scripts start up the domain, and WebLogic Server pods and services.

## Prerequisites

The following prerequisites must be handled prior to running the create domain script:
* Make sure the WebLogic Operator is running.
* Create a Kubernetes namespace for the domain unless the intention is to use the default namespace.
* Create the Kubernetes secrets `username` and `password` of the admin account in the same Kubernetes namespace as the domain.
* Build the Oracle WebLogic image oracle/weblogic:12.2.1.3-developer . Refer to [Oracle WebLogic Server on Docker](https://github.com/oracle/docker-images/tree/master/OracleWebLogic/dockerfiles/12.2.1.3).

## Using the script to create a domain

Make a copy of the `create-domain-inputs.yaml` file, and run the create script, pointing it at your inputs file and an output directory:

```
  ./create-domain.sh \
  -i create-domain-inputs.yaml \
  -o /path/to/output-directory
```

The script will perform the following steps:

* Create a directory for the generated properties and Kubernetes YAML files for this domain if it does not already exist.  The pathname is `/path/to/weblogic-operator-output-directory/weblogic-domains/<domainUID>`. Note that the script fails if the directory is not empty when the `create-domain.sh` script is executed.
* Create a proerties file, `domain.properties`, in the directory that is created above. This properties file will be used to create a sample WebLogic Server domain.
* Build Docker image based on Docker sample [Example Image with a WebLogic Server Domain](https://github.com/oracle/docker-images/tree/master/OracleWebLogic/samples/12213-domain-home-in-image). It will create a sample WebLogic Server domain into the Docker image. You can also run the Docker sample [Example Image with a WebLogic Server Domain](https://github.com/oracle/docker-images/tree/master/OracleWebLogic/samples/12213-domain-home-in-image) manually with the generated `domain.properties` to create domain home image. Note: Oracle recommends keeping the domain home image private in the local repository.
* Create a Kubernetes domain YAML file, `domain.yaml`, in the directory that is created above. This YAML file can be used to create the Kubernetes resource using the `kubectl create -f` or `kubectl apply -f` command.

As a convenience, using the `-e` option, the script can optionally create the domain object, which in turn results in the creation of the corresponding WebLogic Server pods and services as well.

The usage of the create script is as follows:

```
$ sh create-domain.sh -h
usage: create-domain.sh -o dir -i file [-e] [-h]
  -i Parameter inputs file, must be specified.
  -o Output directory for the generated properties and YAML files, must be specified.
  -e Also create the resources in the generated YAML file, optional.
  -h Help

```

If you copy the sample scripts to a different location, make sure that you copy everything in the `<weblogic-kubernetes-operator-project>/kubernetes/samples/scripts` directory together into the target directory, maintaining the original directory hierarchy.

The default domain created by the script has the following characteristics:

* An Administration Server named `admin-server` listening on port `7001`.
* A dynamic cluster named `cluster-1` of size 2.
* Two Managed Servers, named `managed-server1` and `managed-server2`, listening on port `8001`.
* No applications deployed.
* A Derby data source.
* A T3 channel.

The domain creation inputs can be customized by editing `create-domain-inputs.yaml`.

### Configuration parameters
The following parameters can be provided in the inputs file.

| Parameter | Definition | Default |
| --- | --- | --- |
| `adminPort` | Port number for the Administration Server inside the Kubernetes cluster. | `7001` |
| `adminNodePort` | Port number of the Administration Server outside the Kubernetes cluster. | `30701` |
| `adminServerName` | Name of the Administration Server. | `admin-server` |
| `clusterName` | Name of the WebLogic cluster instance to generate for the domain. | `cluster-1` |
| `clusterType` | Type of the WebLogic Cluster. Legal values are `CONFIGURED` or `DYNAMIC`. | `DYNAMIC` |
| `configuredManagedServerCount` | Number of Managed Server instances to generate for the domain. | `2` |
| `domainUID` | Unique ID that will be used to identify this particular domain. Used as the name of the generated WebLogic domain as well as the name of the Kubernetes domain resource. This ID must be unique across all domains in a Kubernetes cluster. This ID cannot contain any character that is not valid in a Kubernetes service name. | `domain1` |
| `exposeAdminNodePort` | Boolean indicating if the Administration Server is exposed outside of the Kubernetes cluster. | `false` |
| `exposeAdminT3Channel` | Boolean indicating if the T3 administrative channel is exposed outside the Kubernetes cluster. | `false` |
| `initialManagedServerReplicas` | Number of Managed Servers to initially start for the domain. | `2` |
| `javaOptions` | Java options for starting the Administration and Managed Servers. A Java option can have references to one or more of the following pre-defined variables to obtain WebLogic domain information: `$(DOMAIN_NAME)`, `$(DOMAIN_HOME)`, `$(ADMIN_NAME)`, `$(ADMIN_PORT)`, and `$(SERVER_NAME)`. | `-Dweblogic.StdoutDebugEnabled=false` |
| `managedServerNameBase` | Base string used to generate Managed Server names. | `managed-server` |
| `managedServerPort` | Port number for each Managed Server. | `8001` |
| `namespace` | Kubernetes namespace in which to create the domain. | `default` |
| `productionModeEnabled` | Boolean indicating if production mode is enabled for the domain. | `true` |
| `serverStartPolicy` | Determines which WebLogic Servers will be started up. Legal values are `NEVER`, `ALWAYS`, `IF_NEEDED`, `ADMIN_ONLY`. | `IF_NEEDED` |
| `t3ChannelPort` | Port for the T3 channel of the NetworkAccessPoint. | `30012` |
| `t3PublicAddress` | Public address for the T3 channel. | `kubernetes` |
| `weblogicCredentialsSecretName` | Name of the Kubernetes secret for the Administration Server's username and password. | `domain1-weblogic-credentials` |
| `includeServerOutInPodLog` | Boolean indicating whether to include server .out to the pod's stdout. | `true` |

Note that the names of the Kubernetes resources in the generated YAML files may be formed with the value of some of the properties specified in the `create-inputs.yaml` file. Those properties include the `adminServerName`, `clusterName` and `managedServerNameBase`. If those values contain any characters that are invalid in a Kubernetes service name, those characters are converted to valid values in the generated YAML files. For example, an uppercase letter is converted to a lowercase letter and an underscore `("_")` is converted to a hyphen `("-")`.

The sample demonstrates how to create a WebLogic domain home and associated Kubernetes resources for a domain that only has one cluster. In addition, the sample provides the capability for users to supply their own scripts to create the domain home for other use cases. The generated domain YAML file could also be modified to cover more use cases.

## Verify the results

The create script will verify that the domain was created, and will report failure if there was any error.  However, it may be desirable to manually verify the domain, even if just to gain familiarity with the various Kubernetes objects that were created by the script.

Note that the example results below uses the `default` Kubernetes namespace. If you are using a different namespace, you need to replace `NAMESPACE` in the example `kubectl` commands with the actual Kubernetes namespace.

### Generated YAML files with the default inputs

The content of the generated `domain.yaml`:

```
# Copyright 2017, 2018, Oracle Corporation and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# This is an example of how to define a Domain resource.
#
apiVersion: "weblogic.oracle/v2"
kind: Domain
metadata:
  name: domain1
  namespace: default
  labels:
    weblogic.resourceVersion: domain-v2
    weblogic.domainUID: domain1
    weblogic.domainName: domain1
spec:
  # The domainUID must be unique across the entire Kubernetes Cluster.   Each WebLogic Domain must
  # have its own unique domainUID.  This does not have to be the same as the Domain Name.  It is allowed
  # to have multiple Domains with the same Domain Name, but they MUST have different domainUID's.
  # The domainUID is also used to identify the Persistent Volume that belongs to/with this Domain.
  domainUID: domain1
  # The WebLogic Domain Name
  domainName: domain1
  # The WebLogic Domain Home
  domainHome: /u01/oracle/user_projects/domains/domain1
  # If the domain home is in the image
  domainHomeInImage: true
  # The Operator currently does not support other images
  image: "12213-domain-home-in-image:latest"
  # imagePullPolicy defaults to "Never"
  imagePullPolicy: "Never"
  # Identify which Secret contains the WebLogic Admin credentials (note that there is an example of
  # how to create that Secret at the end of this file)
  adminSecret:
    name: domain1-weblogic-credentials
  # The name of the Admin Server
  asName: "admin-server"
  # The Admin Server's ListenPort
  asPort: 7001
  # Whether to include the server out file into the pod's stdout, default is true
  includeServerOutInPodLog: true
  # serverStartPolicy legal values are "NEVER", "ALWAYS", "IF_NEEDED", or "ADMIN_ONLY"
  # This determines which WebLogic Servers the Operator will start up when it discovers this Domain
  # - "ALWAYS" will start up all defined servers
  # - "ADMIN_ONLY" will start up only the administration server (no managed servers will be started)
  # - "IF_NEEDED" will start all non-clustered servers, including the administration server and clustered servers up to the replica count
  serverStartPolicy: "IF_NEEDED"
  # adminServer is used to configure the desired behavior for starting the administration server.
  adminServer:
  # serverStartState legal values are "RUNNING" or "ADMIN"
  # "RUNNING" means the listed server will be started up to "RUNNING" mode
  # "ADMIN" means the listed server will be start up to "ADMIN" mode
    serverStartState: "RUNNING"
    # The Admin Server's NodePort
    # nodePort: 30701
    # Uncomment to export the T3Channel as a service
    # exportedNetworkAccessPoints:
    #   T3Channel: {}
    # an (optional) list of environment variable to be set on the server
    env:
    - name: JAVA_OPTIONS
      value: "-Dweblogic.StdoutDebugEnabled=false"
    - name: USER_MEM_ARGS
      value: "-Xms64m -Xmx256m "
    volumes:
    - name: runtime-properties
      hostPath:
        path: "/scratch/weblogic-kubernetes-operator/kubernetes/samples/scripts/create-weblogic-domain/domain-home-in-image/docker-images/OracleWebLogic/samples/12213-domain-home-in-image/properties/docker_run"
    volumeMounts:
    - name: runtime-properties
      mountPath: "/u01/oracle/properties"
  # clusters is used to configure the desired behavior for starting member servers of a cluster.  
  # If you use this entry, then the rules will be applied to ALL servers that are members of the named clusters.
  clusters:
    cluster-1:
      desiredState: "RUNNING"
      replicas: 2
      env:
      - name: JAVA_OPTIONS
        value: "-Dweblogic.StdoutDebugEnabled=false"
      - name: USER_MEM_ARGS
        value: "-Xms64m -Xmx256m "
      volumes:
      - name: runtime-properties
        hostPath:
          path: "/scratch/weblogic-kubernetes-operator/kubernetes/samples/scripts/create-weblogic-domain/domain-home-in-image/docker-images/OracleWebLogic/samples/12213-domain-home-in-image/properties/docker_run"
      volumeMounts:
      - name: runtime-properties
        mountPath: "/u01/oracle/properties"
  # The number of managed servers to start for unlisted clusters
  # replicas: 1

```

### Verify the domain

To confirm that the domain was created, use this command:

```
kubectl describe domain DOMAINUID -n NAMESPACE
```

Replace `DOMAINUID` with the `domainUID` and `NAMESPACE` with the actual namespace.

Here is an example of the output of this command:

```
$ kubectl describe domain domain1
Name:         domain1
Namespace:    default
Labels:       weblogic.domainName=base_domain
              weblogic.domainUID=domain1
              weblogic.resourceVersion=domain-v2
Annotations:  <none>
API Version:  weblogic.oracle/v1
Kind:         Domain
Metadata:
  Cluster Name:        
  Creation Timestamp:  2018-10-31T12:59:15Z
  Generation:          0
  Resource Version:    1314703
  Self Link:           /apis/weblogic.oracle/v1/namespaces/default/domains/domain1
  UID:                 c52f5e11-dd0c-11e8-9238-fa163e855ac8
Spec:
  Admin Secret:
    Name:   domain1-weblogic-credentials
  As Name:  admin-server
  As Port:  7001
  Cluster Startup:
    Cluster Name:   cluster-1
    Desired State:  RUNNING
    Env:
      Name:    JAVA_OPTIONS
      Value:   -Dweblogic.StdoutDebugEnabled=false
      Name:    USER_MEM_ARGS
      Value:   -Xms64m -Xmx256m
    Replicas:  2
  Clusters:
  Domain Home In Image:  false
  Domain Name:           base_domain
  Domain UID:            domain1
  Env:
  Export T 3 Channels:
    T3Channel
  Exported Network Access Points:
  Image:              store/oracle/weblogic:12.2.1.3
  Image Pull Policy:  IfNotPresent
  Image Pull Secret:
  Liveness Probe:
  Managed Servers:
  Readiness Probe:
  Server Startup:
    Desired State:  RUNNING
    Env:
      Name:         JAVA_OPTIONS
      Value:        -Dweblogic.StdoutDebugEnabled=false
      Name:         USER_MEM_ARGS
      Value:        -Xms64m -Xmx256m
    Node Port:      30701
    Server Name:    admin-server
  Startup Control:  AUTO
  Storage:
    Predefined:
      Claim:  domain1-weblogic-sample-pvc
Status:
  Conditions:
    Last Transition Time:  2018-10-31T13:01:34.786Z
    Reason:                ServersReady
    Status:                True
    Type:                  Available
  Servers:
    Health:
      Activation Time:  2018-10-31T13:07:14.472Z
      Overall Health:   ok
      Subsystems:
    Node Name:     xxxxxxxx
    Server Name:   admin-server
    State:         RUNNING
    Cluster Name:  cluster-1
    Health:
      Activation Time:  2018-10-31T13:01:05.100Z
      Overall Health:   ok
      Subsystems:
    Node Name:     xxxxxxxx
    Server Name:   managed-server1
    State:         RUNNING
    Cluster Name:  cluster-1
    Health:
      Activation Time:  2018-10-31T13:01:05.190Z
      Overall Health:   ok
      Subsystems:
    Node Name:    xxxxxxxx
    Server Name:  managed-server2
    State:        RUNNING
  Start Time:     2018-10-31T12:59:15.244Z
Events:           <none>
```

In the `Status` section of the output, the available servers and clusters are listed.  Note that if this command is issued very soon after the script finishes, there may be no servers available yet, or perhaps only the Administration Server but no Managed Servers.  The operator will start up the Administration Server first and wait for it to become ready before starting Managed Servers.

### Verify pods

The following command can be used to see the pods running the servers:

```
kubectl get pods -n NAMESPACE
```

Here is an example of the output of this command:

```
$ kubectl get pods
NAME                                         READY     STATUS    RESTARTS   AGE
domain1-admin-server                         1/1       Running   0          1m
domain1-managed-server1                      1/1       Running   0          8m
domain1-managed-server2                      1/1       Running   0          8m
```

### Verify services

The following command can be used to see the services for the domain:

```
kubectl get services -n NAMESPACE
```

Here is an example of the output of this command:
```
$ kubectl get services
NAME                                        TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)           AGE
domain1-admin-server                        NodePort    10.96.206.134    <none>        7001:30701/TCP    23m
domain1-admin-server-extchannel-t3channel   NodePort    10.107.164.241   <none>        30012:30012/TCP   22m
domain1-cluster-cluster-1                   ClusterIP   10.109.133.168   <none>        8001/TCP          22m
domain1-managed-server1                     ClusterIP   None             <none>        8001/TCP          22m
domain1-managed-server2                     ClusterIP   None             <none>        8001/TCP          22m
```

## Delete domain

Th generated YAML file in the `/path/to/weblogic-operator-output-directory/weblogic-domains/<domainUID>` directory can also be used to delete the Kubernetes resource. The following command can be used to delete domain:

```
$ kubectl delete -f domain.yaml

```