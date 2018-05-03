mobile-tax-credits-summary
=============================================

[ ![Download](https://api.bintray.com/packages/hmrc/releases/mobile-tax-credits-summary/images/download.svg) ](https://bintray.com/hmrc/releases/mobile-tax-credits-summary/_latestVersion)

Allows users to view their tax credits information and perform a renewal.

Requirements
------------

The following services are exposed from the micro-service.

Please note it is mandatory to supply an Accept HTTP header to all below services with the value ```application/vnd.hmrc.1.0+json```. 

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/income/:nino/tax-credits/tax-credits-summary``` | GET | Fetch the Tax Credits Summary object for a given NINO. [More...](docs/tax-credits-summary.md)|
| ```/income/:nino/tax-credits/tax-credits-decision``` | GET | Fetch the Tax Credits renewal status. [More...](docs/tax-credits-decision.md)  |

# Sandbox
All the above endpoints are accessible on sandbox with `/sandbox` prefix on each endpoint,e.g.
```
    GET /sandbox/income/:nino/tax-credits/tax-credits-summary
```

# Definition
API definition for the service will be available under `/api/definition` endpoint.
See definition in `/conf/api-definition.json` for the format.

# Version
Version of API need to be provided in `Accept` request header
```
Accept: application/vnd.hmrc.v1.0+json
```