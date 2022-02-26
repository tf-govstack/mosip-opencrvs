#!/bin/bash

read -p "OPENCRVS CLIENT ID : " CLIENT_ID
if [ -z $CLIENT_ID ]; then echo "Error!"; exit 1; fi
read -p "OPENCRVS CLIENT SECRET : " CLIENT_SECRET
if [ -z $CLIENT_SECRET ]; then echo "Error!"; exit 1; fi
read -p "OPENCRVS SHA SECRET : " SHA_SECRET
if [ -z $SHA_SECRET ]; then echo "Error!"; exit 1; fi

cat $1 | sed "s/___CLIENT_ID___/$(echo $CLIENT_ID | base64)/g" | sed "s/___CLIENT_SECRET___/$(echo $CLIENT_SECRET | base64)/g" | sed "s/___SHA_SECRET___/$(echo $SHA_SECRET | base64)/g" | kubectl apply -f -
