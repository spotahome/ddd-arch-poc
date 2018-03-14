#!/bin/bash

topic=$1

if [[ -n "$topic" ]]; then
	docker-compose exec broker kafka-topics --create --topic $topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181
else
	echo "Use: ./create_topic.sh [topic_name]"
fi
