#!/bin/bash

read -p "OPENCRVS CLIENT ID : " CLIENT_ID
if [ -z $CLIENT_ID ]; then echo "Error!"; exit 1; fi
read -p "OPENCRVS CLIENT SECRET : " CLIENT_SECRET
if [ -z $CLIENT_SECRET ]; then echo "Error!"; exit 1; fi
read -p "OPENCRVS SHA SECRET : " SHA_SECRET
if [ -z $SHA_SECRET ]; then echo "Error!"; exit 1; fi
read -p "OPENCRVS DOMAIN NAME : " OPENCRVS_ROOT_DOMAIN
if [ -z $OPENCRVS_ROOT_DOMAIN ]; then echo "Error!"; exit 1; fi
MOSIP_API_PUBLIC_HOST=$(kubectl get cm global -o jsonpath={.data.mosip-api-host})

cat $1 | sed "s/___CLIENT_ID___/$(echo -n $CLIENT_ID | base64)/g" | sed "s/___CLIENT_SECRET___/$(echo -n $CLIENT_SECRET | base64)/g" | sed "s/___SHA_SECRET___/$(echo -n $SHA_SECRET | base64)/g" | sed "s/___OPENCRVS_ROOT_DOMAIN___/$OPENCRVS_ROOT_DOMAIN/g" | sed "s/___MOSIP_API_PUBLIC_HOST___/$MOSIP_API_PUBLIC_HOST/g" | kubectl apply -n $2 -f -
