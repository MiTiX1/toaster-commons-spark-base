package dev.toaster.commons

import dev.toaster.commons.DefaultConfig.{appName, bootstrapServers, includeHeaders, logLevel, master, schemaRegistryUrl, specificAvroReader, startingOffsets, subscribeTopic}
import org.apache.kafka.common.serialization.Deserializer
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter
import scala.util.{Failure, Success, Try}

abstract class SparkBase[K, V]{

  private var isConfigured = false
  private var innerKeyDeserializer: Option[Deserializer[K]] = None
  private var innerValueDeserializer: Option[Deserializer[V]] = None

  private lazy val spark: SparkSession = {
    val session = SparkSession
      .builder
      .master(master)
      .appName(appName)
      .getOrCreate()
    session.sparkContext.setLogLevel(logLevel)
    session
  }

  private val configMap: java.util.Map[String, String] = Map(
    "schema.registry.url" -> schemaRegistryUrl,
    "specific.avro.reader" -> specificAvroReader,
  ).asJava

  def configure(keyDeserializer: Deserializer[K], valueDeserializer: Deserializer[V]): Unit = {
    innerKeyDeserializer = Some(keyDeserializer)
    innerValueDeserializer = Some(valueDeserializer)
    innerKeyDeserializer match {
      case Some(k: Deserializer[K]) => k.configure(configMap, true)
      case Some(k) => throw new RuntimeException("Key deserializer could not be initialised")
      case None => throw new RuntimeException("Key deserializer should be defined")
    }
    innerValueDeserializer match {
      case Some(v: Deserializer[V]) => v.configure(configMap, false)
      case Some(v) => throw new RuntimeException("Value deserializer could not be initialised")
      case _ => throw new RuntimeException("Value deserializer should be defined")
    }
    isConfigured = true
  }

  private def subscribe: DataFrame = spark
    .readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", bootstrapServers)
    .option("subscribe", subscribeTopic)
    .option("startingOffsets", startingOffsets)
    .option("includeHeaders", includeHeaders)
    .load()

  private def deserialize(keyBytes: Array[Byte], valueBytes: Array[Byte], headersRaw: Option[Seq[Row]]): Option[(K, V, Map[String, String])] = {
    Try {
      val key = innerKeyDeserializer.get.deserialize(subscribeTopic, keyBytes)
      val value = innerValueDeserializer.get.deserialize(subscribeTopic, valueBytes)
      val headers: Map[String, String] =
        headersRaw.getOrElse(Seq.empty).flatMap { h =>
          for {
            keyBytes <- Option(h.getAs[String]("key"))
            valueBytes <- Option(h.getAs[Array[Byte]]("value"))
          } yield {
            val key = keyBytes
            val value = new String(valueBytes, "UTF-8")
            key -> value
          }
        }.toMap
      (key, value, headers)
    } match {
      case Success(value) => Option(value)
      case Failure(e) =>
        println(s"Error while deserializing, $e")
        None
    }
  }

  private def extractMessageFields(df: DataFrame): Seq[Option[(K, V, Map[String, String])]] = {
    df.collect().toSeq.map { row =>
      val keyBytes = row.getAs[Array[Byte]]("key")
      val valueBytes = row.getAs[Array[Byte]]("value")
      val headersRaw = Option(row.getAs[Seq[Row]]("headers"))
      deserialize(keyBytes, valueBytes, headersRaw)
    }
  }

  private def awaitTermination(): Unit = spark.streams.awaitAnyTermination()

  def consumeFromKafka(f: (Seq[Option[(K, V, Map[String, String])]], Long) => Unit): Unit = {
    if (!isConfigured) throw new RuntimeException("SparBase should be configured first")
    subscribe.writeStream
      .foreachBatch { (batchDf: DataFrame, batchId: Long ) =>
        val messages = extractMessageFields(batchDf)
        f(messages, batchId)
      }
      .start()
    awaitTermination()
  }
}
