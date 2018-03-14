#!/bin/bash

topic=$1

if [[ -n "$topic" ]]; then
	docker-compose exec broker kafka-topics --delete --topic $topic --zookeeper zookeeper:2181
else
	echo "Use: ./delete_topic.sh [topic_name]"
fi
