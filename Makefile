start:
	docker-compose up --build &
	echo "Sleeping 20 seconds ..."
	sleep 20
	docker-compose exec broker kafka-topics --create --topic events.user --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper:2181

clean:
	docker-compose stop; docker-compose down; docker-compose rm;

