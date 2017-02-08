
# API Gateway

API Gateway proxies HTTP requests to downstream services.
This is a **provisional project**.

[![Build Status](https://travis-ci.org/hmrc/api-gateway.svg?branch=master)](https://travis-ci.org/hmrc/api-gateway) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gateway/images/download.svg) ](https://bintray.com/hmrc/releases/api-gateway/_latestVersion)

This is a placeholder README.md for a new repository

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
    
# Testing API Gateway

There are a number of inter-operating components that are required to be running in order to test the platform locally. Most of these can be started with the Service Manager.

sm --start DATASTREAM API_DEFINITION API_EXAMPLE_MICROSERVICE 

In addition, the following need starting from the commandline:

## third-party-application

``sbt run -Dhttp.port=9607 -DDev.skipWso2=true``


