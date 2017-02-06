# Secure the API with OAuth 2.0 and authentication Service

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://github.com/ibm-cloud-architecture/refarch-cloudnative*

This project provides the artifact to authenticate the API user as well as enable OAuth 2.0 authorization for all OAuth protected APIs in the BlueCompute reference application. IBM API Connect OAuth provider delegates authentication and authorization to this component, which verifies credentials using the [Customer Microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-customer). The project contains the following components:

 - Spring Boot application that handles user authentication
 - Uses Spring Feign Client to get an instance of the Customer Microservice from Eureka registry and validate login credentials
 - Passes customer identity back to API Connect for identity propagation
 
The application uses API Connect OAuth 2.0 provider Public/Password grant type. For detail of how API Connect supports OAuth 2.0, please reference the IBM Redbook [Getting Started with IBM API Connect: Scenarios Guide](https://www.redbooks.ibm.com/redbooks.nsf/RedpieceAbstracts/redp5350.html?Open)

# Prerequisites

- Docker installation
- [Eureka](https://github.com/ibm-cloud-architecture/refarch-cloudnative-netflix-eureka) 
- [Customer microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-customer)

# Deploy to BlueMix

You can use the following button to deploy the Authentication microservice to Bluemix, or you can follow the instructions manually below.

[![Create BlueCompute Deployment Toolchain](https://console.ng.bluemix.net/devops/graphics/create_toolchain_button.png)](https://console.ng.bluemix.net/devops/setup/deploy?repository=https://github.com/ibm-cloud-architecture/refarch-cloudnative-auth.git)


# Deploy the Authentication Service:

In the sample application, the API Connect OAuth provider relies on the Authentication microservice to validate user credentials.  The Authentication service is deployed as a container group with a public route that connects to Eureka to 

## Build the Docker container

1. Build the application.  This builds both the WAR file for the Orders REST API and also the Spring Sidecar application:

   ```
   # ./gradlew build
   ```

2. Copy the binaries to the docker container
   
   ```
   # ./gradlew docker
   ```

3. Build the docker container
   ```
   # cd docker
   # docker build -t auth-microservice .
   ```

## Run the Docker container locally (optional)

Execute the following to run the Docker container locally.  Note that you require a local [Eureka](https://github.com/ibm-cloud-architecture/refarch-cloudnative-netflix-eureka) instance and a local [Customer microservice](https://github.com/ibm-cloud-architecture/refarch-cloudnative-micro-customer).  Be sure to replace `<Eureka URL>` with the URL for Eureka.  

Note that the authentication microservice does not register with Eureka (`eureka.client.registerWithEureka=false`) but fetches the registery (`eureka.client.fetchRegistry=true`)

```
# docker run -d --name auth-microservice -P \
  -e eureka.client.fetchRegistry=true \
  -e eureka.client.registerWithEureka=false \
  -e eureka.client.serviceUrl.defaultZone=<Eureka URL> \
  auth-microservice
```

## Validate the (local) Authentication service (optional)

For a user `foo` with password `bar`, get the authentication string:

```
# echo -n "foo:bar" | base64 
Zm9vOmJhcg==
```

Use this string to pass in the authorization header:

```
curl -i -H "Authorization: Basic Zm9vOmJhcg==" http://localhost:8080/authenticate
```

## Deploy the container group on Bluemix

1. Tag and push the auth-microservice to Bluemix:
   ```
   # docker tag auth-microservice registry.ng.bluemix.net/$(cf ic namespace get)/auth-microservice
   # docker push registry.ng.bluemix.net/$(cf ic namespace get)/auth-microservice
   ```

2. Deploy the container group on Bluemix

   Be sure to replace `<Eureka URL>` with the URL for Eureka.  Note the `name` passed to the command, a public route will be mapped to the container group.

   ```
   # cf ic group create \
     --name auth-microservice \
     --publish 8080 \
     -e eureka.client.fetchRegistry=true \
     -e eureka.client.registerWithEureka=false \
     -e eureka.client.serviceUrl.defaultZone=<Eureka URL> \
     --desired 1 \
     --min 1 \
     --max 3 \
     registry.ng.bluemix.net/$(cf ic namespace get)/auth-microservice
   ```

## Validate the Authentication service on Bluemix

For a user `foo` with password `bar`, get the authentication string:

```
# echo -n "foo:bar" | base64 
Zm9vOmJhcg==
```

Use this string to pass in the authorization header.  This command should return HTTP 200 which indicates the authentication was successful.

```
curl -i -H "Authorization: Basic Zm9vOmJhcg==" https://auth-microservice.mybluemix.net/authenticate
HTTP/1.1 200 OK
```
