
# API Gateway

API Gateway proxies HTTP requests to downstream services.
This is a **provisional project**.

[![Build Status](https://travis-ci.org/hmrc/api-gateway.svg?branch=master)](https://travis-ci.org/hmrc/api-gateway) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gateway/images/download.svg) ](https://bintray.com/hmrc/releases/api-gateway/_latestVersion)

This is a placeholder README.md for a new repository

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

# Testing API Gateway

There are a number of inter-operating components that are required to be running in order to test the platform locally. Most of these can be started with the Service Manager.

sm --start DATASTREAM API_EXAMPLE_MICROSERVICE API_PUBLISHER SERVICE_LOCATOR THIRD_PARTY_DELEGATED_AUTHORITY API_SCOPE API_DOCUMENTATION_RAML_FRONTEND

In addition, the following need starting from the commandline:

## third-party-application

``sbt run -Dhttp.port=9607 -DDev.skipWso2=true``

## api-definition

``sbt run -Dhttp.port=9604 -Drun.mode=Stub``

## api-gateway

This API, but in Dev mode::

    sbt run -Drun.mode=Dev

## Hosts File / NGINX

In order for the api-publisher to find the "vagrant-host" server which Service Manager started services have their hostname set to, add the following to your ``/etc/hosts`` file:

    127.0.0.1	localhost  api-documentation-raml-frontend.service vagrant-host

NGINX will also need to be running locally with the following config in ``/etc/nginx/...`` to allow the RAML documents to pick up the shared configuration:

  server {
	listen 80;
	listen [::]:80;

	server_name api-documentation-raml-frontend.service;

	location /api-documentation {
		proxy_pass http://127.0.0.1:9681/api-documentation;
	}
  }

## WSO2

This needs setting up and runnng locally since the api-definition service publishes to it. Information on configuring this is here:
    https://github.tools.tax.service.gov.uk/HMRC/wso2-api-manage

Alternatively, start API_DEFINITION in Stub mode, and run the mocked-external-services-stub which will stub out the WSO2 endpoints. Details to follow once mocked-external-services-stub has been added to service manager.
