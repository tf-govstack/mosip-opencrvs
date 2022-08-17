#!/usr/bin/env bash

docker run -d --rm --name=zookeeper -p 2181:2181 -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:3.8
sleep 5s
docker run -d --rm \
    --name=kafka \
    -p 9092:9092 \
    -e ALLOW_PLAINTEXT_LISTENER=yes \
    -e KAFKA_CFG_ZOOKEEPER_CONNECT=172.17.0.1:2181 \
    -e KAFKA_CFG_ADVERTISED_LISTENERS="PLAINTEXT://localhost:9092" \
    bitnami/kafka:3.2

