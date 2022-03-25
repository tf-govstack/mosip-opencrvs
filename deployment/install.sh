#!/bin/sh
# Install opencrvs-mediator
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=opencrvs
CHART_VERSION=1.2.0

if [ -z $OPENCRVS_AUTH_URL ]; then read -p "Give Opencrvs Auth Url : " OPENCRVS_AUTH_URL ; fi
if [ -z $OPENCRVS_WEBHOOKS_URL ]; then read -p "Give Opencrvs Webhooks Url : " OPENCRVS_WEBHOOKS_URL; fi
if [ -z $OPENCRVS_CALLBACK_HOST_BIRTH ]; then read -p "Give Opencrvs Birth Callback Host : " OPENCRVS_CALLBACK_HOST_BIRTH; fi
if [ -z $OPENCRVS_CALLBACK_URI_BIRTH ]; then read -p "Give Opencrvs Birth Callback Uri : " OPENCRVS_CALLBACK_URI_BIRTH; fi
if [ -z $OPENCRVS_CLIENT_ID ]; then read -p "Give Opencrvs Client id : " OPENCRVS_CLIENT_ID; fi
if [ -z $OPENCRVS_CLIENT_SECRET ]; then read -p "Give Opencrvs Client secret : " OPENCRVS_CLIENT_SECRET; fi
if [ -z $OPENCRVS_CLIENT_SHA_SECRET ]; then read -p "Give Opencrvs Client sha secret : " OPENCRVS_CLIENT_SHA_SECRET; fi

echo Create $NS namespace
kubectl create ns $NS

echo Istio Injection Enabled
kubectl label ns $NS istio-injection=enabled --overwrite
helm repo add mosip https://mosip.github.io/mosip-helm
helm repo update

echo Copy Configmaps.
./copy_cm.sh

echo Copy Secrets.
./copy_secrets.sh
kubectl -n $NS create secret generic opencrvs-client-creds \
  --from-literal=opencrvs_client_id=$OPENCRVS_CLIENT_ID \
  --from-literal=opencrvs_client_secret_key=$OPENCRVS_CLIENT_SECRET \
  --from-literal=opencrvs_client_sha_secret=$OPENCRVS_CLIENT_SHA_SECRET

echo Installing mosip-side opencrvs-mediator...
helm -n $NS install opencrvs-mediator mosip/opencrvs-mediator \
  --version $CHART_VERSION \
  --set mediator.opencrvs.authUrl=$OPENCRVS_AUTH_URL \
  --set mediator.opencrvs.webhooksUrl=$OPENCRVS_WEBHOOKS_URL \
  --set mediator.opencrvs.birthCallback.uri=$OPENCRVS_CALLBACK_URI_BIRTH \
  --set mediator.opencrvs.birthCallback.host=$OPENCRVS_CALLBACK_HOST_BIRTH \
  --set mediator.opencrvs.client.secretName="opencrvs-client-creds" \
  --set mediator.mosipOpencrvsKeycloakClientId="mosip-resident-client" \
  --set istio.existingGateway="istio-system/public" \
  --wait

echo Installing regproc-opencrvs-stage...
helm -n $NS install regproc-opencrvs-stage mosip/regproc-opencrvs \
  --version $CHART_VERSION \
  --wait
