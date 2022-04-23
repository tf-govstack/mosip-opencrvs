# Deployment

## Overview
This document describe deployment of `mosip-side-mediator` and `registration-processor-opencrvs-stage`.

## Prerequisites
- The following command line utilities.
  - `psql`, `kubectl`,`helm`,`bash`, `curl`, `jq`

## Installation
- Set up `mosip_opencrvs` db:
  - Navigate to [db_scripts/mosip_opencrvs](../db_scripts/mosip_opencrvs).
  - Configure deploy.properties, with required secrets and hostname.
  - Then run:
    ```sh
    ./deploy.sh deploy.properties
    ```
- Create a new transaction type `OPENCRVS_NEW` in `mosip_regprc/transaction_type` database table.
- Onboard a new credential type partner (using [partner onboarding scripts](https://github.com/mosip/mosip-onboarding/tree/master)), with;
  - name like `opencrvs-partner`
  - appropriate certificates from OpenCRVS
  - an _auth_policy_ that has only `UIN` in _sharableAttributes_.
  - _credential_type_ is `opencrvs`.
- Furthermore, change the following settings for the `opencrvs-partner` client (that was just created), in keycloak admin console, in `Mosip` realm.
  - Enable _Direct Access Grants Enabled_.
  - Enable _Service Accounts Enabled_.
  - Enable _Authorization Enabled_.
  - Change _Valid Redirect URIs_ to `*`.
- Apart from creating the partner keycloak client, create a new user with the same username as the partner name (that was previously given), with any password.
- Apart from the certificates obtained from OpenCRVS, create a certificate key pair for MOSIP, this public certificate will later be shared with OpenCRVS.
- Run the following to install the mediator and components (The script will prompt for inputs):
    ```sh
    ./install.sh <cluster-kubeconfig-file>
    ```
  - OR Pass the following environment variables to the above script, if it is not desired to prompt for inputs:
    ```sh
    export OPENCRVS_AUTH_URL=
    export OPENCRVS_RECEIVE_CREDENTIAL_URL=
    export OPENCRVS_CLIENT_ID=
    export OPENCRVS_CLIENT_SECRET=
    export OPENCRVS_CLIENT_SHA_SECRET=
    export MOSIP_OPENCRVS_PARTNER_CLIENT_ID=
    export MOSIP_OPENCRVS_PARTNER_CLIENT_SHA_SECRET=
    export MOSIP_OPENCRVS_PARTNER_USERNAME=
    export MOSIP_PRIVATE_KEY_PATH=
    export OPENCRVS_PUBLIC_KEY_PATH=
    ./install.sh <cluster-kubeconfig-file>
    ```
- Share the details with OpenCRVS: auth_url(mosip keycloak url), partner_client_id, partner_client_sha_secret, partner_username, partner_password.
- Share MOSIP public Certificate (that was created above).

## Uploading sample birth data
- Run the following in current directory, to upload sample birth data to mediator:
  ```sh
  curl -XPOST \
    -H "content-type: application/json" \
    -d @samples/sampleDataFromOpencrvs2.json \
    https://<opencrvs-hostname-for-mosip-mediator>/<mosip-mediator-webhooks-uri>
  ```
- Replace url with `http://localhost:4545/webhooks` if running locally.

## Uninstallation
- Run:
    ```sh
    ./delete.sh <cluster-kubeconfig-file>
    ```
