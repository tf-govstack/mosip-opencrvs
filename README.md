# MOSIP OpenCRVS Integration - MOSIP Side Mediator

## Overview
- This repo contains MOSIP side of components in the MOSIP OpenCRVS integration. Namely:
  - _MOSIP side OpenCRVS Mediator_ ([`mosip-side-opencrvs-mediator`](./mediator)) receives data from OpenCRVS, creates registration packet, and uploads it to registration processor.
  - _OpenCRVS Registration Processor Stage_ ([`registration-processor-opencrvs-stage`](./registration-processor-opencrvs-stage)) is added as the last stage in MOSIP registration-processor pipeline, to issue the credentials back to OpenCRVS, after registration is successful.
  - _MOSIP OpenCRVS Print Stage_ ([`opencrvs-print`](./opencrvs-print)) is an ephemeral component to demonstrate a credential being issued and printed successfully.
- This repo is not to be confused with [OpenCRVS side MOSIP Mediator](https://github.com/opencrvs/mosip-mediator/tree/master), which is also part of MOSIP OpenCRVS integration, that receives the credential(UIN) issued by MOSIP, and processes it as required.

_This is work-in-progress_.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).

## Deployment
- [mosip-side-opencrvs-mediator](https://github.com/mosip/mosip-helm/tree/1.2.0/charts/opencrvs-mediator) and [registration-processor-opencrvs-stage](https://github.com/mosip/mosip-helm/tree/1.2.0/charts/regproc-opencrvs) are available as installable helm chart.
- Refer to [deployment instructions](./deployment), for installing this project on a kubernetes cluster using the above helm charts.

## Notes
Refer to [Notes](./notes.md).