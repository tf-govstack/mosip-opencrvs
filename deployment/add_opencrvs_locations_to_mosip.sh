#!/usr/bin/env bash

if [ -z $OPENCRVS_AUTH_URL ]; then read -p "Give Opencrvs Auth Url : " OPENCRVS_AUTH_URL ; fi
if [ -z $OPENCRVS_LOCATIONS_URL ]; then read -p "Give Opencrvs Locations Url : " OPENCRVS_LOCATIONS_URL ; fi

if [ -z $OPENCRVS_CLIENT_ID ]; then read -p "Give Opencrvs Client id : " OPENCRVS_CLIENT_ID; fi
if [ -z $OPENCRVS_CLIENT_SECRET ]; then read -p "Give Opencrvs Client secret : " OPENCRVS_CLIENT_SECRET; fi

#if [ -z $MOSIP_AUTH_URL ]; then read -p "Give MOSIP Auth Url : " MOSIP_AUTH_URL ; fi
#if [ -z $MOSIP_MASTERDATA_URL ]; then read -p "Give MOSIP Masterdata Url : " MOSIP_MASTERDATA_URL ; fi

#if [ -z $MOSIP_CLIENT_ID ]; then read -p "Give Opencrvs Client id : " MOSIP_CLIENT_ID; fi
#if [ -z $MOSIP_CLIENT_SECRET ]; then read -p "Give Opencrvs Client secret : " MOSIP_CLIENT_SECRET; fi

get_names_from_ids() {
  # 1 arg is all locations json
  # 2 arg is list of ids
  # returns list of names
  for loc_id in $2; do
    echo $1 | jq -r ".\"${loc_id}\".name"
  done
}

get_codes_from_names() {
  # 1 arg is list of names
  # returns list of codes
  for name in $1; do
    value="OPC${name:0:2}${name: -1}"
    echo "${value^^}"
  done
}

get_children_of_parent() {
  # 1 arg is all locations json
  # 2 arg is parent id
  # returns all children of parent
  keys=$(echo $1 | jq -r 'keys[]')
  for loc_id in $keys; do
    part_of=$(echo $1 | jq -r ".\"${loc_id}\".partOf")
    if [ "$part_of" = "Location/$2" ] ; then
      echo $loc_id
    fi
  done
}

opencrvs_token=$(curl -s -H "Content-Type: application/json" "${OPENCRVS_AUTH_URL}" -d "{\"client_id\":\"${OPENCRVS_CLIENT_ID}\",\"client_secret\":\"${OPENCRVS_CLIENT_SECRET}\"}" | jq -r '.token')
#mosip_token=$(curl -s "${MOSIP_AUTH_URL}" -d "client_id=${MOSIP_CLIENT_ID}&client_secret=${MOSIP_CLIENT_SECRET}&grant_type=client_credentials" | jq -r '.access_token')

req_time=$(date -u +\"%Y-%m-%dT%H:%M:%S.000Z\")

#echo mosip_token $mosip_token
#echo opencrvs_token $opencrvs_token

opencrvs_locations=$(curl -s -H"Authorization: Bearer ${opencrvs_token}" "${OPENCRVS_LOCATIONS_URL}" | jq '.data')
echo "total count - $(echo $opencrvs_locations | jq 'length')"

opencrvs_state_ids=$(get_children_of_parent "${opencrvs_locations}" "0")

for state_id in $opencrvs_state_ids; do
  state_name=$(get_names_from_ids "${opencrvs_locations}" "${state_id}")
  state_code=$(get_codes_from_names "${state_name}")

  opencrvs_district_ids=$(get_children_of_parent "${opencrvs_locations}" "$state_id")
  opencrvs_districts=$(get_names_from_ids "${opencrvs_locations}" "${opencrvs_district_ids}")

  opencrvs_district_codes=$(get_codes_from_names "${opencrvs_districts}")

  opencrvs_districts_array=(${opencrvs_districts//$'\n'/ })
  opencrvs_district_codes_array=(${opencrvs_district_codes//$'\n'/ })

  #curl -s -H "Content-type: application/json" -H "Cookie: Authorization=${mosip_token}" "${MOSIP_MASTERDATA_URL}/locations" -d '{
  #  "id": "string",
  #  "version": "string",
  #  "requesttime": "'"$req_time"'",
  #  "metadata": {},
  #  "request": {
  #    "code": "",
  #    "name": "",
  #    "hierarchyLevel": 0,
  #    "hierarchyName": "",
  #    "parentLocCode": "",
  #    "langCode": "",
  #    "isActive": true
  #  }
  #}'
  echo "Req time $req_time"
  echo "This State - $state_name - $state_code - Districts -"
  for ((i=0;i<=${#opencrvs_districts_array[@]};i++));do
    echo "${opencrvs_districts_array[i]} - ${opencrvs_district_codes_array[i]}"
  done
done