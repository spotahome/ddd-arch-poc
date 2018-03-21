package com.anistal.streamexample

import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

object AkkaStream extends App {

  implicit val system = ActorSystem("stream-example")

  val decider: Supervision.Decider = { exception =>
    exception.printStackTrace
    Supervision.Resume
  }

  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer = ActorMaterializer(materializerSettings)
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val bootstrapServer = "localhost:9092"

  val messageDigestInstance = MessageDigest.getInstance("MD5")
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(bootstrapServer)
    .withGroupId("group")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")

  val calculateMD5 = Flow[ConsumerRecord[String, String]].map { event =>
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = event.value().getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  val calculateMD5String = Flow[String].map { event =>
    val m = java.security.MessageDigest.getInstance("MD5")
    val b = event.getBytes("UTF-8")
    m.update(b, 0, b.length)
    new java.math.BigInteger(1, m.digest()).toString(16)
  }

  // @formatter:off
  val g = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder =>
      import GraphDSL.Implicits._

      val A: Outlet[ConsumerRecord[String, String]] =
        builder.add(Consumer.plainSource(consumerSettings, Subscriptions.topics("bookings"))).out
      val B: FlowShape[ConsumerRecord[String, String], String] = builder.add(calculateMD5.async)
      val C: Inlet[Any] = builder.add(Sink.ignore).in

      // Graph
      A ~> B ~> C

      ClosedShape
  })
  // @formatter:on

  g.run()

}
