
# API Gateway

API Gateway proxies HTTP requests to downstream services.

[![Build Status](https://travis-ci.org/hmrc/api-gateway.svg?branch=master)](https://travis-ci.org/hmrc/api-gateway) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gateway/images/download.svg) ](https://bintray.com/hmrc/releases/api-gateway/_latestVersion)

IMPORTANT: This is a bespoke implementation designed to eventually replace `WSO2`. 
At the moment only PRODUCTION credentials work. SANDBOX credentials are not handled yet.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

# Testing API Gateway

There are a number of inter-operating components that are required to be running in order to test the platform locally. Most of these can be started with the Service Manager.

``sm --start DATASTREAM API_DEFINITION API_EXAMPLE_MICROSERVICE API_PUBLISHER SERVICE_LOCATOR THIRD_PARTY_DELEGATED_AUTHORITY API_SCOPE``

In addition, the following need starting from the commandline:

## third-party-application

``sbt run -Dhttp.port=9607 -DDev.skipWso2=true``

## api-gateway

This API, but in Dev mode:

    sbt run -Drun.mode=Dev

## WSO2

This needs setting up and runnng locally since the api-definition service publishes to it. Information on configuring this is here:
    https://github.tools.tax.service.gov.uk/HMRC/wso2-api-manager

Alternatively, start API_DEFINITION in Stub mode (`sbt run -Dhttp.port=9604 -Drun.mode=Stub`), and run the `mocked-external-services-stub` which will stub out the WSO2 endpoints. 
Details to follow once `mocked-external-services-stub` has been added to service manager.

## Tests
The tests include unit tests and integration tests.
Some of the integration tests require `MongoDB` to run.
Thus, remember to start up MongoDB if you want to run the integration tests locally.
In order to run the tests, use this command line:
```
sbt test it:test
```



