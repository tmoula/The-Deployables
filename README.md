# Deployables Planning

 

 

## Planned

 

Add the readme

Update readme with PlantUML diagram of system components

Update readme Add to readme AI citation(s)

Update readme with this task list

Update readme database instructions

 

Figure out port-forward with Postgress on GKE to connect local code to DB

Figure out port-forward with RabbitMQ on GKE to connect local code to DB

 

Pushing images to Harbor on GKE

Secret for Harbor image container pulls

Add ingress to access user interface (see doc)

Add namespace(s) to GKE cluster (manually or with a YAML), e.g. "deps-"

Deploy K8s manifests to GKE cluster

 

Integration test on GKE - ensuring emails are sent.

 

Use GitHub Secrets and K8s Secrets - No secrets in the repo

Use K8s ConfigMaps for non-secret environment variables

 

80% unit test coverage

 

 



 

 

## In Progress
CI builds and pushes all container images to Harbor on GKE (EC)
Obtain data from a public API. (EC)
CD pulls all container images from Harbor as well as all YAMLS on GKE (EC)

 

Bug with emails (TM)

 

Can't connect to Postgress using psql - why?  (JJ)

 

## Completed

 

Postgress engine install on GKE w/ instructions (JJ)

Basic services

Container image builds

Postgress hookups

RabbitMQ integration

AI integration

 

## Maybe Later

 

Helm chart for deployment

 
