#!/bin/bash

number_of_events=$1

if [[ -n "$number_of_events" ]]; then
	for (( i=1; i<=$number_of_events; i++))
	do
	   curl http://localhost:8080/$i
	done
else
	echo "Use: ./generate_events.sh [number_of_events]"
fi
