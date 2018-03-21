package com.dddarchpoc

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.{Base64, UUID}

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream._
import com.sksamuel.avro4s.{AvroInputStream, RecordFormat}
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

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

  private val schemaRegistryClient = new CachedSchemaRegistryClient("http://localhost:8081", 1000)

  val decider: Supervision.Decider = { exception =>
    exception.printStackTrace
    Supervision.Resume
  }

  val format = RecordFormat[Event]

  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer = ActorMaterializer(materializerSettings)
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val bootstrapServer = "localhost:9092"

  val messageDigestInstance = MessageDigest.getInstance("MD5")
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new KafkaAvroDeserializer(schemaRegistryClient))
    .withProperty("schema.registry.url", "http://localhost:8081")
    .withBootstrapServers(bootstrapServer)
    .withGroupId(UUID.randomUUID().toString)
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")

  Consumer.committableSource(consumerSettings, Subscriptions.topics("userevents"))
    .map {
      _.record.value().asInstanceOf[GenericRecord]
    }.runForeach { record =>
    val event = format.from(record)
    val payload = Base64.getDecoder.decode(event.payload)
    val in = new ByteArrayInputStream(payload)
    val input = AvroInputStream.binary[User](in)
    val user = input.iterator.toSeq.head

    println(user)
  }
}
