#!/bin/sh
# Uninstalls opencrvs-mediator
## Usage: ./delete.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=opencrvs
while true; do
    read -p "Are you sure you want to delete opencrvs-mediator helm chart?(Y/n) " yn
    if [ $yn = "Y" ]
      then
        helm -n $NS delete opencrvs-mediator
        kubectl -n $NS delete secret opencrvs-client-creds
        break
      else
        break
    fi
done
