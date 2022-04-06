## Notes
- Dummy *centerId* and *machineId* are being used in properties. These are not part of `mosip_master` db. This is possible because CMD validation has been skipped for opencrvs packets in the camel route.
- Keymanager is being used to encrypt and decrypt packet, the same above *ref_id* (`centerId_machineId`) is being used.
  Keymanager will already take care of creating the appropriate keys if this ref_id is not present in its db.
- `app_id: KERNEL, ref_id: SIGN` key is being used to sign the packet.
- The mediator app has the following components;
  - Producer:
    - receives birth notification with data from OpenCRVS.
    - puts it in kafka.
  - Receiver:
    - keeps polling kafka for any new data.
    - checks db if the packet is already proccessed, or if rid is already assigned.
    - processes the packet. creates it uploads it to packet receiver.
    - republishes it to kafka, if any error in this process.
    - updates status and *rid* in db.
- For a packet generated from this opencrvs process (exact process name used is `OPENCRVS_NEW`), the following stages
  are skipped in the camel flow:
  - CMD validator. Because, this centerid and machineid don't exactly exist as part of mosip.
  - Operator & Supervisor validator, not relevant when packet is generated without any operator/supervisor.
  - Demo-dedupe & Bio-dedupe stages are skipped.
  - Introducer authentication stage is present, but no introducer data is being sent, so this will be skipped.
  - Will modify the above step with proper introducer data later.
  - For structure of data being received from OpenCRVS, refer to
    their [Webhooks Integration document](https://documentation.opencrvs.org/opencrvs-core/docs/technology/webhooks/).
- Currently, while creating packet from OpenCRVS data, dummy values are assigned for the following properties. TODO:
  Change this with real data.
  - `addressLine1`,`addressLine2`,`addressLine3`,`region`,`province`,`city`,`zone`,`postalCode`,`phone`
  - Check these properties under `opencrvs-default.properties` in mosip-config
    ```
    opencrvs.data.dummy.address.line1
    opencrvs.data.dummy.address.line2
    opencrvs.data.dummy.address.line3
    opencrvs.data.dummy.region
    opencrvs.data.dummy.province
    opencrvs.data.dummy.city
    opencrvs.data.dummy.zone
    opencrvs.data.dummy.postal.code
    opencrvs.data.dummy.phone
    ```
- These properties are added so that packetmanager can corelate this process with this source. In opencrvs-default.properties:
  ```
  opencrvs.birth.process.type=OPENCRVS_NEW
  ```
  In application-default.properties:
  ```
  packetmanager.default.priority=source:REGISTRATION_CLIENT\/process:BIOMETRIC_CORRECTION|NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:OPENCRVS\/process:OPENCRVS_NEW
  provider.packetreader.opencrvs=source:OPENCRVS,process:OPENCRVS_NEW,classname:io.mosip.commons.packet.impl.PacketReaderImpl
  provider.packetwriter.opencrvs=source:OPENCRVS,process:OPENCRVS_NEW,classname:io.mosip.commons.packet.impl.PacketWriterImpl
  ```
- The following property has been added for mandatory-attributes check
  ```
  mosip.kernel.idobjectvalidator.mandatory-attributes.reg-processor.opencrvs_new=IDSchemaVersion
  ```
- The camel flow xml file has been added in the following property, in registration-default.properties.
  ```
  camel.secure.active.flows.file.names=registration-processor-camel-routes-new-default.xml,registration-processor-camel-routes-update-default.xml,registration-processor-camel-routes-activate-default.xml,registration-processor-camel-routes-res-update-default.xml,registration-processor-camel-routes-deactivate-default.xml,registration-processor-camel-routes-lost-default.xml,registration-processor-camel-routes-res-reprint-default.xml,registration-processor-camel-routes-biometric-correction-default.xml,registration-processor-camel-routes-opencrvs_new-default.xml
  ```
- The OPECNRVS_NEW process type had to be included in the main-processes list, in
  registration-processor-default.properties
  ```
  registration.processor.main-processes=NEW,UPDATE,LOST,RES_UPDATE,ACTIVATE,DEACTIVATE,OPENCRVS_NEW
  ```
- Consider adding OPENCRVS_NEW proccess as part of `mosip_regprc/transaction_type` db_scripts. (For now this is created as part of installation)
- While creating `opencrvs-partner`, `opencrvs` credential_type has been used. The same will be used by the opencrvs side mediator while receiving credential. The following property has been changed to include this as well, in partner-management-default.properties. 
  ```
  pmp.allowed.credential.types=auth,qrcode,euin,reprint,vercred,opencrvs
  ```
- Added these props in opencrvs-default.properties, which tell the mediator to recreate packet in case of failure while
  creating the packet. WIP.
  ```
  opencrvs.reproduce.on.error=false
  opencrvs.reproduce.on.error.delay.ms=10000
  ```
- In general in MOSIP, the idea is to not have any de-duplication whatsoever, in case of child packet. Discuss this further.
- Discuss a longterm model for Registrations that would happen through partners in MOSIP. And refit this implementation to that model.
- Discuss an infra model with OpenCRVS, on how the OpenCRVS webhook and MOSIP websub would communicate with each other over a secured private channel.
  - From mosip side, we would want OpenCRVS webhook callback to happen on the private wireguard channel.
  - Plus the callback contains Authorization.
  - Plus the data is encrypted.
- Discuss how `zone`,`province`,`city`,`postal code` and other basic data about a country (masterdata) can be in sync between MOSIP and OpenCRVS.

Misc Dev Notes:
- Create `mosip-opencrvs-client` in keycloak. Assign this client all the roles that are required to create and upload packets. And use that in properties. TODO
- Create a duplicate print stage called opencrvs-print stage, which will add additional opencrvs data in the credential request. TODO
- Onboard an `opencrvs-partner`, create a similar client and user in keycloak. DONE
  - Use this above partner and subscribe to websub, for uin generated event. DONE
- Update postgres-init to include `mosip-opencrvs` db. CLOSED: WONT DO.
- Use `kernel-auth-adapter`, and remove the dummy adapter in code. TODO.
- Create docker, helm chart in mosip-helm, and add ci to github. DONE.
- Create testcases. TODO.
- Analyze with sonar cloud. Publish to mvn repo & snapshots to ossrh. TODO.
