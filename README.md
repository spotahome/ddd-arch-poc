DDD / Event Sourcing Arch
=========================

Overview
--------
PoC about an event sourcing architecture using the Confluent Platform.

The objetive of this PoC is to show:

- Integrate 2 heterogeneous components (one in PHP and another one in Scala).
- All messages between components will use messages using Avro as a data serialization system.
- All messages will be published and consumed using Apache Kafka.

