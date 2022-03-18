# MOSIP OpenCRVS Integration

## Overview
This repo contains components that connect OpenCRVS to MOSIP. 

_This is work-in-progress_.

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).

## Deployment
- This project is available as installable [helm chart](https://github.com/mosip/mosip-helm/tree/1.2.0/charts/opencrvs-mediator).
- Refer to [deployment instructions](./deployment), for installing the project on a kubenetes cluster using the above helm chart. 

## Notes
- Dummy *centerId* and *machineId* are being used in properties. These are not part of `mosip_master` db.
- Keymanager is being used to encrypt and decrypt packet, the same above *ref_id* (`centerId_machineId`) is being used. Keymanager will already take care of creating the appropriate keys if this ref_id is not present in its db.
- `app_id: KERNEL, ref_id: SIGN` key is being used to sign the packet.
- The mediator app has the following components;
  - Producer:
    - receives birth notification with data from Opencrvs.
    - puts it in kafka.
  - Receiver:
    - keeps polling kafka for any new data.
    - checks db if the packet is already proccessed, or if rid is already assigned.
    - processes the packet. creates it uploads it to packet receiver.
    - republishes it to kafka, if any error in this process.
    - updates status and *rid* in db.
- For a packet generated from this opencrvs process (exact process name used is `OPENCRVS_NEW`), the following stages are skipped in the camel flow:
  - CMD validator. Because, this centerid and machineid don't exact as part of mosip.
  - Operator & Supervisor validator, not relevant when packet is generated without any operator/supervisor.
  - Bio-dedupe stage is present, but is disabled.
  - Introducer authentication stage is also present, but no introducer data is being sent, so this will be skipped.
  - Will modify the above step proper introducer data later.
- For structure of data being received from Opencrvs, refer to their [Webhooks Integration document](https://documentation.opencrvs.org/opencrvs-core/docs/technology/webhooks/).

Refer to [Dev Notes](./dev-notes.md).