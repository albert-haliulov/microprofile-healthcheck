# LAB - Checking the health of microservices on Kubernetes

Learn how to check the health of microservices on Kubernetes by setting up readiness probes to inspect MicroProfile Health Check endpoints.

## What you’ll learn

You will learn how to create a health check endpoint for your microservices. Then, you will configure Kubernetes to use this endpoint to keep your microservices running smoothly.

MicroProfile Health allows services to report their health, and it publishes the overall health status to a defined endpoint. A service reports `UP` if it is available and reports `DOWN` if it is unavailable. MicroProfile Health reports an individual service status at the endpoint and indicates the overall status as `UP` if all the services are `UP`. A service orchestrator can then use the health statuses to make decisions.

Kubernetes provides liveness and readiness probes that are used to check the health of your containers, you will work with readiness probes. These probes can check certain files in your containers, check a TCP socket, or make HTTP requests. MicroProfile Health exposes a health endpoint on your microservices. Kubernetes polls the endpoint as specified by the probes to react appropriately to any change in the microservice’s status. 

## Prerequisites

**You must be connected to the internet to perform this tasks**

- **MacOS, Linux on x86_64 architecture only**
    - If you are running on Windows, please prepare virtual box/machine with Ubuntu
    - Ubuntu: https://www.ubuntu.com/download/desktop
    - Virtual Box: https://www.virtualbox.org/wiki/Downloads
- **Git client**
    - MacOS X: https://git-scm.com/download/mac
    - Linux/Unix: https://git-scm.com/download/linux
- **Eclispe**
    - Download and install Eclipse IDE for Enterprise Java Developers: https://www.eclipse.org/downloads/packages/
- **OpenLiberty**
    - Download the latest version of OpenLiberty: https://openliberty.io/downloads/
- **Docker**
     - Version 17.06 minimum: https://docs.docker.com/install/ 
- **IBM Cloud Kubernetes Cluster**
    - Create K8s cluster in IBM Cloud (a free or standard cluster with 1 worker node in it).
    - Install CLI, create a private regristry, setup up created cluster envrironment.
    - You can use tutorial to do these steps: https://cloud.ibm.com/docs/containers?topic=containers-cs_cluster_tutorial#cs_cluster_tutorial

## Getting started

- Open Eclipse IDE

- Find and Install IBM Liberty Developer Tools for building and deploying JEE apps to OpenLiberty:
    - Open Eclipse Marketplace -> Menu->Help->Eclipse Marketplace
    - Find Liberty package -> Write `Liberty` in `Find` field and click `Go` button
    - Install Liberty Developer Tools 19.0.0.3 -> Select appropriate package from the list and click `Install` button
    - Restart eclipse


## Download and Import projects into your workspace

File->Import->Project Interchange

## Play with applications
 >>>>>> Here need to put part to Look at the source code and play with apps deploying into Liberty servers - name-serer & ping-server respectively <<<<<

The two microservices you will work with are called `name` and `ping`. The `name` microservice displays a brief greeting and the name of the container that it runs in. The `ping` microservice pings the Kubernetes Service that encapsulates the pod running the `name` microservice. The `ping` microservice demonstrates how communication can be established between pods inside a cluster.

The `ping` microservice should only be healthy when `name` is available. To add this check to the `/health` endpoint, you will create a class implementing the `HealthCheck` interface.

>>>Create PingHealth class.
io/openliberty/guides/ping/PingHealth.java
````
package io.openliberty.guides.ping;

import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.openliberty.guides.ping.client.NameClient;
import io.openliberty.guides.ping.client.UnknownUrlException;

@Health
@ApplicationScoped
public class PingHealth implements HealthCheck {
	@Inject
    @ConfigProperty(name = "NAME_HOSTNAME", defaultValue="localhost")
    private String hostname;

    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named(hostname);
        if (isNameServiceReachable()) {
            builder = builder.up();
        } else {
            builder = builder.down();
        }

        return builder.build();
    }

    private boolean isNameServiceReachable() {
        try {
            NameClient client = RestClientBuilder
                .newBuilder()
                .baseUrl(new URL("http://" + hostname + ":9080/api"))
                .register(UnknownUrlException.class)
                .build(NameClient.class);

            client.getContainerName();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
````

This health check verifies that the `name` microservice is available at `http://name-service:9080/api`. The `name-service` host name is only accessible from inside the cluster, you can’t access it yourself. If it’s available, then it returns an `UP` status. Similarly, if it’s unavailable then it returns a `DOWN` status. When the status is `DOWN` the microservice is considered to be unhealthy.

The health check for the `name` microservice has already been implemented. It has been setup to become unhealthy for 60 seconds when a specific endpoint is called. This endpoint has been provided for you to observe the results of an unhealthy pod and how Kubernetes reacts.



````
$ curl -X GET http://localhost:9080/api/name
Hello! I'm application NameService
````

````
$ curl -X GET http://localhost:9081/api/ping/localhost
pong
````

````
$ curl -X GET http://localhost:9080/health
{"checks":[{"data":{},"name":"isAlive","state":"UP"}],"outcome":"UP"}
````

````
curl -X POST http://localhost:9080/api/name/unhealthy
Application NameService is now unhealthy...
````

````
$ curl -X GET http://localhost:9080/api/name
ERROR: Service is currently in maintenance.
````

````
$ curl -X GET http://localhost:9080/health
{"checks":[{"data":{},"name":"isAlive","state":"DOWN"}],"outcome":"DOWN"}
````

````
$ curl -X GET http://localhost:9081/api/ping/localhost
ERROR: Service is currently in maintenance.
````

````
$ curl -X GET http://localhost:9081/health
{"checks":[{"data":{},"name":"localhost","state":"DOWN"}],"outcome":"DOWN"}
````


## Build applications Name and Ping



## Build docker images

Build `name:1.0` image with `Name` service

````
$ cd Name/

$ docker build -t name:1.0 .
Sending build context to Docker daemon  162.8kB
Step 1/3 : FROM open-liberty
 ---> d447e301fe7c
Step 2/3 : COPY --chown=1001:0 server.xml /config
 ---> 07efaf2293b4
Step 3/3 : COPY --chown=1001:0 *.war /config/apps/name.war
 ---> 30e2bec82e9f
Successfully built 30e2bec82e9f
Successfully tagged name:1.0
````
Build `ping:1.0` image with `Ping` service

````
$ cd Ping/

$ docker build -t ping:1.0 .
Sending build context to Docker daemon  1.163MB
Step 1/3 : FROM open-liberty
 ---> d447e301fe7c
Step 2/3 : COPY --chown=1001:0 server.xml /config
 ---> 9277e6fab8b0
Step 3/3 : COPY --chown=1001:0 *.war /config/apps/ping.war
 ---> a9868d823b4b
Successfully built a9868d823b4b
Successfully tagged ping:1.0
````

Check that images have been created:

````
$ docker images
REPOSITORY                               TAG                 IMAGE ID            CREATED             SIZE
name                                     1.0                 7c3773c712a7        16 hours ago        535MB
ping                                     1.0                 fea53a6caf11        16 hours ago        535MB
````

Now we are ready for the next step - run our containers.

## Run & Play with docker containers

Run docker containers from prepared images with `name` and `ping` services. In order to start containes we will use `docker-compose`.

Docker Compose is a tool for defining and running multi-container Docker applications. It uses YAML files to configure the application's services and performs the creation and start-up process of all the containers with a single command.

Let's look at `docker-compose.yaml`:

````
version: '3'

services:
   name-service:
     image: name:1.0
     ports:
       - 9080:9080
   
   ping-service:
     image: ping:1.0
     ports:
       - 9081:9081
     environment:
       NAME_HOSTNAME: name-service
     depends_on:
      - name-service
````

We used version 3 notation: `https://docs.docker.com/compose/compose-file/`. As you can see, there are two services defined `name-service` and `ping-service` which will be created from our docker images `name:1.0` and `ping:1.0`.
Ports mapped in the same manner as you usually use with `docker run` command. 

If you open `io.openliberty.guides.ping.PingHealth` class which we created in previous steps, you can see the ConfigProperty `NAME_HOSTNAME`. This environment variable initialized by `name-service` which is the name of service.

````
public class PingHealth implements HealthCheck {
	@Inject
    @ConfigProperty(name = "NAME_HOSTNAME", defaultValue="localhost")
    private String hostname;
````

And at the bottom of `docker-compose.yaml` we used `depends_on`. It is express dependency between services. `docker-compose up` command starts services in dependency order. In our case, `name-service` are started before `ping-service`. 

Let's start it:

````
$ docker-compose up -d
Creating network "microprofile-healthcheck_default" with the default driver
Creating microprofile-healthcheck_name-service_1_7e0ffbcdc769 ... done
Creating microprofile-healthcheck_ping-service_1_12e3084076cd ... done
````

Check that the contaners have been started:

````
$ docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS                                        NAMES
89d9112eb276        ping:1.0            "/opt/ol/helpers/run…"   28 seconds ago      Up 26 seconds       9080/tcp, 9443/tcp, 0.0.0.0:9081->9081/tcp   microprofile-healthcheck_ping-service_1_36774b188a5b
a814a31c2cbc        name:1.0            "/opt/ol/helpers/run…"   29 seconds ago      Up 27 seconds       0.0.0.0:9080->9080/tcp, 9443/tcp             microprofile-healthcheck_name-service_1_13efd498dbe0

````

Now you can play with services:

Notice that our `name-service` responing as a container, not an application as was before. Check the application class `io.openliberty.guides.name.NameResource` to understand this behaviour. 
````
$ curl -X GET http://localhost:9080/api/name
Hello! I'm container a814a31c2cbc
````

We used `name-service` to ping service, because now it is accesible by `name-service` hostname.
````
$ curl -X GET http://localhost:9081/api/ping/name-service
pong
````

And check the healthcheck stuff, that it is working in containers before we deploy it to kubernetes:

````
$ curl -X GET http://localhost:9080/health
{"checks":[{"data":{},"name":"isAlive","state":"UP"}],"outcome":"UP"}

$ curl -X POST http://localhost:9080/api/name/unhealthy
Container a814a31c2cbc is now unhealthy...

$ curl -X GET http://localhost:9080/health
{"checks":[{"data":{},"name":"isAlive","state":"DOWN"}],"outcome":"DOWN"}

$ curl -X GET http://localhost:9081/api/ping/name-service
ERROR: Service is currently in maintenance.

$ curl -X GET http://localhost:9081/health
{"checks":[{"data":{},"name":"name-service","state":"DOWN"}],"outcome":"DOWN"}
````

Wait a minute in order to `name-service` returned from maintenance state and check health again:
````
$ curl -X GET http://localhost:9081/api/ping/name-service
pong

curl -X GET http://localhost:9081/health
{"checks":[{"data":{},"name":"name-service","state":"UP"}],"outcome":"UP"}
````

We have done it. 

Let's deploy it to kubernetes now and check readiness Probe how it can handle healthcheck of our services.

## Creating and preparing your cluster for deployment

Go through the IBM Cloud Kubernetes Service tutorial `https://cloud.ibm.com/docs/containers?topic=containers-cs_cluster_tutorial#cs_cluster_tutorial` for creating a Kubernetes cluster, installing the CLI, creating a private registry and setting up your cluster environment.

## Configuring readiness probes

Readiness probes are responsible for determining that your application is ready to accept requests. If it’s not ready, traffic won’t be routed to the container.

>>>>Create the kubernetes configuration file.
kubernetes.yaml
````
apiVersion: apps/v1
kind: Deployment
metadata:
  name: name-deployment
  labels:
    app: name
spec:
  replicas: 2
  selector:
    matchLabels:
      app: name
  template:
    metadata:
      labels:
        app: name
    spec:
      containers:
      - name: name-container
        image: name:1.0-SNAPSHOT
        ports:
        - containerPort: 9080
        # name probe
        readinessProbe:
          httpGet:
            path: /health
            port: 9080
          initialDelaySeconds: 15
          periodSeconds: 5
          failureThreshold: 1
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ping-deployment
  labels:
    app: ping
spec:
  selector:
    matchLabels:
      app: ping
  template:
    metadata:
      labels:
        app: ping
    spec:
      containers:
      - name: ping-container
        image: ping:1.0-SNAPSHOT
        ports:
        - containerPort: 9081
        env:
        - name: NAME_HOSTNAME
          value: name-service
        # ping probe
        readinessProbe:
          httpGet:
            path: /health
            port: 9081
          initialDelaySeconds: 15
          periodSeconds: 5
          failureThreshold: 1
---
apiVersion: v1
kind: Service
metadata:
  name: name-service
spec:
  type: NodePort
  selector:
    app: name
  ports:
  - protocol: TCP
    port: 9080
    targetPort: 9080
    nodePort: 31000
---
apiVersion: v1
kind: Service
metadata:
  name: ping-service
spec:
  type: NodePort
  selector:
    app: ping
  ports:
  - protocol: TCP
    port: 9081
    targetPort: 9081
    nodePort: 32000
````

Open `kubernetes.yaml` file and see - the readiness probes are configured for the containers running the `name` and `ping` microservices.

The readiness probes are configured to poll the `/health` endpoint. The readiness probe determines the READY status of the container as seen in the `kubectl get pods` output. The `initialDelaySeconds` field defines how long the probe should wait before it starts to poll so the probe does not start making requests before the server has started. The `failureThreshold` option defines how many times the probe should fail before the state should be changed from ready to not ready. The `periodSeconds` option defines how often the probe should poll the given endpoint.

## Deploying microservices into Kubernetes cluster

Change path to root dirtectory of project - where kubernetes.yaml is located. Deploy application by using this chart.

````
kubectl apply -f kubernetes.yaml
````

## Play with deployed application
>>>>>>Here need to put part - Play with deployed application

Use the following command to view the status of the pods. There will be two name pods and one `ping` pod, later you’ll observe their behaviour as the `name` pods become unhealthy.

````
$kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   1/1       Running   0          59s
name-deployment-694c7b74f7-lrlf7   1/1       Running   0          59s
ping-deployment-cf8f564c6-nctcr    1/1       Running   0          59s
````
Wait until the pods are ready. After the pods are ready, you will make requests to your services.

Navigate to `http://[hostname]:31000/api/name` and observe a response similar to `Hello! I’m container name-deployment-5f868854bf-2rhdq`. Replace `[hostname]` with the IP address or host name of your Kubernetes cluster. The readiness probe ensures the READY state won’t be `1/1` until the container is available to accept requests. Without a readiness probe, you may notice an unsuccessful response from the server. This scenario can occur when the container has started, but the application server hasn’t fully initialized. With the readiness probe, you can be certain the pod will only accept traffic when the microservice has fully started.

Similarly, navigate to `http://[hostname]:32000/api/ping/name-service` and observe a response with the content `pong`.


## Changing the ready state of the name microservice

An endpoint has been provided under the `name` microservice to set it to an unhealthy state in the health check. The unhealthy state will cause the readiness probe to fail. Use the `curl` command to invoke this endpoint by making a POST request to `http://[hostname]:31000/api/name/unhealthy` — if `curl` is unavailable then use a tool such as **Postman**.

````
$ curl -X POST http://[hostname]:31000/api/name/unhealthy
Application NameService is now unhealthy...
````

Run the following command to view the state of the pods:
````
$ kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   1/1       Running   0          1m
name-deployment-694c7b74f7-lrlf7   0/1       Running   0          1m
ping-deployment-cf8f564c6-nctcr    1/1       Running   0          1m
````
You will notice that one of the two name pods is no longer in the ready state. Navigate to `http://[hostname]:31000/api/name`. Observe that your request will still be successful because you have two replicas and one is still healthy.

## Observing the effects on the ping microservice

Wait until the `name` pod is ready again. Make two POST requests to `http://[hostname]:31000/api/name/unhealthy`. If you see the same pod name twice, make the request again until you see that the second pod has been made unhealthy. You may see the same pod twice because there’s a delay between a pod becoming unhealthy and the readiness probe noticing it. Therefore, traffic may still be routed to the unhealthy service for approximately 5 seconds. Continue to observe the output of `kubectl get pods`. You will see both pods are no longer ready. During this process, the readiness probe for the `ping` microservice will also fail. Observe it’s no longer in the ready state either.
````
$ curl -X POST http://[hostname]:31000/api/name/unhealthy
Application NameService is now unhealthy...
$ curl -X POST http://[hostname]:31000/api/name/unhealthy
Application NameService is now unhealthy...
````
First, both `name` pods will no longer be ready because the readiness probe failed.
````
$ kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   0/1       Running   0          5m
name-deployment-694c7b74f7-lrlf7   0/1       Running   0          5m
ping-deployment-cf8f564c6-nctcr    1/1       Running   0          5m
````
Next, the `ping` pod is no longer ready because the readiness probe failed. The probe failed because `name-service` is now unavailable.
````
$ kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   0/1       Running   0          6m
name-deployment-694c7b74f7-lrlf7   0/1       Running   0          6m
ping-deployment-cf8f564c6-nctcr    0/1       Running   0          6m
````

Then, the `name` pods will start to recover.

````
$ kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   1/1       Running   0          6m
name-deployment-694c7b74f7-lrlf7   0/1       Running   0          6m
ping-deployment-cf8f564c6-nctcr    0/1       Running   0          6m
```
Finally, you will see all of the pods have recovered.

```
$ kubectl get pods
NAME                               READY     STATUS    RESTARTS   AGE
name-deployment-694c7b74f7-hcf4q   1/1       Running   0          6m
name-deployment-694c7b74f7-lrlf7   1/1       Running   0          6m
ping-deployment-cf8f564c6-nctcr    1/1       Running   0          6m
```




