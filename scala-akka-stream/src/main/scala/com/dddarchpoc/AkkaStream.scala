package com.dddarchpoc

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.{Base64, UUID}

import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerMessage, ProducerSettings, Subscriptions}
import akka.stream._
import akka.stream.javadsl.Sink
import akka.stream.scaladsl.Source
import com.sksamuel.avro4s.{AvroInputStream, RecordFormat}
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.{KafkaAvroDeserializer, KafkaAvroSerializer}
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.Future

case class Event(persistenceId: String,
                 eventId: String,
                 creationDate: String,
                 payloadName: String,
                 payloadVersion: String,
                 payload: String)

case class User(id: String,
                name: String,
                email: String)

object AkkaStream extends App {

  implicit val system = ActorSystem("stream-example")

  val SchemaRegistryUrl = "http://localhost:8081"
  val SourceTopicName = "userevents"
  val SinkTopicName = "users"
  val BootstrapServer = "localhost:9092"
  val UserFormat = RecordFormat[User]

  private val schemaRegistryClient = new CachedSchemaRegistryClient(SchemaRegistryUrl, 1000)

  val decider: Supervision.Decider = { exception =>
    exception.printStackTrace
    Supervision.Resume
  }

  val format = RecordFormat[Event]

  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer = ActorMaterializer(materializerSettings)

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val messageDigestInstance = MessageDigest.getInstance("MD5")

  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new KafkaAvroDeserializer(schemaRegistryClient))
    .withProperty("schema.registry.url", SchemaRegistryUrl)
    .withBootstrapServers(BootstrapServer)
    .withGroupId(UUID.randomUUID().toString)
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")

  val producerSettings = ProducerSettings(system, new StringSerializer, new KafkaAvroSerializer(schemaRegistryClient))
    .withProperty("schema.registry.url", SchemaRegistryUrl)
    .withBootstrapServers(BootstrapServer)

  Consumer.committableSource(consumerSettings, Subscriptions.topics(SourceTopicName))
    .map { message =>
      val record = message.record.value().asInstanceOf[GenericRecord]
      val event = format.from(record)
      val payload = Base64.getDecoder.decode(event.payload)
      val in = new ByteArrayInputStream(payload)
      val input = AvroInputStream.binary[User](in)
      val user = input.iterator.toSeq.head

      val genericUser = UserFormat.to(user)
      
      ProducerMessage.Message(new ProducerRecord[String, Object](
        SinkTopicName, genericUser), message.committableOffset)

    }.runWith(Producer.commitableSink(producerSettings))




}
