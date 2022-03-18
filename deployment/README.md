# Opencrvs Mediator Deployment

## Prerequisites
- The following command line utilities.
  - `psql`, `kubectl`,`helm`,`bash`, `curl`, `jq`

## Installation
- Setting up db;
  - Navigate to [db_scripts/mosip_opencrvs](../db_scripts/mosip_opencrvs).
  - Configure deploy.properties, with required secrets and hostname.
  - Then run:
    ```sh
    ./deploy.sh deploy.properties
    ```
- Run the following to install the mediator and components (The script will prompt for inputs):
    ```sh
    ./install.sh <cluster-kubeconfig-file>
    ```
  OR
- Pass the following environment variables to the above script, if it is not desired to prompt for inputs:
    ```sh
    export OPENCRVS_AUTH_URL=
    export OPENCRVS_WEBHOOKS_URL=
    export OPENCRVS_CLIENT_ID=
    export OPENCRVS_CLIENT_SECRET=
    export OPENCRVS_CLIENT_SHA_SECRET=
    export OPENCRVS_CALLBACK_HOST_BIRTH=
    export OPENCRVS_CALLBACK_URI_BIRTH=
    ./install.sh <cluster-kubeconfig-file>
    ```

## Uploading sample birth data
- Run the following in current directory, to upload sample birth data to mediator:
  ```sh
  curl -XPOST \
    -H "content-type: application/json" \
    -d @samples/sampleDataFromOpencrvs2.json \
    https://<hostname-for-mediator>/opencrvs-mediator/birth
  ```
- Replace url with `http://localhost:4545/birth` if running locally.


## Uninstallation
- Run:
    ```sh
    ./delete.sh <cluster-kubeconfig-file>
    ```
