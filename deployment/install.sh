#!/bin/sh
# Install opencrvs-mediator
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=opencrvs
CHART_VERSION=12.0.2

if [ -z $OPENCRVS_AUTH_URL ]; then read -p "Give Opencrvs Auth Url : " OPENCRVS_AUTH_URL ; fi
if [ -z $OPENCRVS_RECEIVE_CREDENTIAL_URL ]; then read -p "Give Opencrvs Receive Uin on Birth Url : " OPENCRVS_RECEIVE_CREDENTIAL_URL ; fi

if [ -z $OPENCRVS_CLIENT_ID ]; then read -p "Give Opencrvs Client id : " OPENCRVS_CLIENT_ID; fi
if [ -z $OPENCRVS_CLIENT_SECRET ]; then read -p "Give Opencrvs Client secret : " OPENCRVS_CLIENT_SECRET; fi
if [ -z $OPENCRVS_CLIENT_SHA_SECRET ]; then read -p "Give Opencrvs Client sha secret : " OPENCRVS_CLIENT_SHA_SECRET; fi

if [ -z $MOSIP_OPENCRVS_PARTNER_CLIENT_ID ]; then read -p "Give MOSIP OpenCRVS Partner Client id : " MOSIP_OPENCRVS_PARTNER_CLIENT_ID; fi
if [ -z $MOSIP_OPENCRVS_PARTNER_CLIENT_SECRET ]; then read -p "Give MOSIP OpenCRVS Partner Client secret : " MOSIP_OPENCRVS_PARTNER_CLIENT_SECRET; fi
if [ -z $MOSIP_OPENCRVS_PARTNER_CLIENT_SHA_SECRET ]; then read -p "Give a random MOSIP OpenCRVS Partner Client sha secret : " MOSIP_OPENCRVS_PARTNER_CLIENT_SHA_SECRET; fi

if [ -z $MOSIP_PRIVATE_KEY_PATH ]; then read -p "Give MOSIP OpenCRVS Mediator Private Key Path : " MOSIP_PRIVATE_KEY_PATH; fi
if [ -z $OPENCRVS_PUBLIC_KEY_PATH ]; then read -p "Give OpenCRVS Public Cert Path : " OPENCRVS_PUBLIC_KEY_PATH; fi

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
kubectl -n $NS delete --ignore-not-found=true secret opencrvs-client-creds
kubectl -n $NS create secret generic opencrvs-client-creds \
  --from-literal=opencrvs_client_id=$OPENCRVS_CLIENT_ID \
  --from-literal=opencrvs_client_secret_key=$OPENCRVS_CLIENT_SECRET \
  --from-literal=opencrvs_client_sha_secret=$OPENCRVS_CLIENT_SHA_SECRET

kubectl -n $NS delete --ignore-not-found=true secret opencrvs-partner-client-creds
kubectl -n $NS create secret generic opencrvs-partner-client-creds \
  --from-literal=mosip_opencrvs_partner_client_id=$MOSIP_OPENCRVS_PARTNER_CLIENT_ID \
  --from-literal=mosip_opencrvs_partner_client_secret=$MOSIP_OPENCRVS_PARTNER_CLIENT_SECRET \
  --from-literal=mosip_opencrvs_partner_client_sha_secret=$MOSIP_OPENCRVS_PARTNER_CLIENT_SHA_SECRET

kubectl -n $NS delete --ignore-not-found=true secret opencrvs-partner-certs-keys
kubectl -n $NS create secret generic opencrvs-partner-certs-keys \
  --from-file=opencrvs-pub.key=$OPENCRVS_PUBLIC_KEY_PATH \
  --from-file=mosip-priv.key=$MOSIP_PRIVATE_KEY_PATH

echo Installing mosip-side opencrvs-mediator...
helm -n $NS install opencrvs-mediator mosip/opencrvs-mediator \
  --version $CHART_VERSION \
  --set mediator.opencrvs.authUrl=$OPENCRVS_AUTH_URL \
  --set mediator.opencrvs.clientSecretName="opencrvs-client-creds" \
  --set mediator.opencrvs.partnerClientSecretName="opencrvs-partner-client-creds" \
  --set mediator.opencrvs.receiveCredentialUrl=$OPENCRVS_RECEIVE_CREDENTIAL_URL \
  --set mediator.opencrvs.certsKeysSecretName="opencrvs-partner-certs-keys" \
  --set mediator.mosipOpencrvsKeycloakClientId="mosip-resident-client" \
  --set istio.existingGateway="istio-system/public" \
  --wait

echo Installing regproc-opencrvs-stage...
helm -n $NS install regproc-opencrvs-stage mosip/regproc-opencrvs \
  --version $CHART_VERSION \
  --wait
