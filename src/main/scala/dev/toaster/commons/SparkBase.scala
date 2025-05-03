package dev.toaster.commons

import dev.toaster.commons.DefaultConfig.{appName, bootstrapServers, includeHeaders, logLevel, master, schemaRegistryUrl, specificAvroReader, startingOffsets}
import org.apache.kafka.common.serialization.Deserializer
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

import scala.jdk.CollectionConverters.mapAsJavaMapConverter
import scala.util.{Failure, Success, Try}

abstract class SparkBase {
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

  private def configureDeserializers[K, V](keyDeserializer: Deserializer[K], valueDeserializer: Deserializer[V]): Unit = {
    keyDeserializer.configure(configMap, true)
    valueDeserializer.configure(configMap, false)
  }

  private def subscribe(topic: String): DataFrame = spark
    .readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", bootstrapServers)
    .option("subscribe", topic)
    .option("startingOffsets", startingOffsets)
    .option("includeHeaders", includeHeaders)
    .load()

  private def deserialize[K, V](
    topic: String,
    keyBytes: Array[Byte],
    valueBytes: Array[Byte],
    headersRaw: Option[Seq[Row]],
    keyDeserializer: Deserializer[K],
    valueDeserializer: Deserializer[V]
  ): Option[(K, V, Map[String, String])] = {
    Try {
      val key = keyDeserializer.deserialize(topic, keyBytes)
      val value = valueDeserializer.deserialize(topic, valueBytes)
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

  private def extractMessageFields[K, V](
    topic: String,
    df: DataFrame,
    keyDeserializer: Deserializer[K],
    valueDeserializer: Deserializer[V]
  ): Seq[Option[(K, V, Map[String, String])]] = {
    df.collect().toSeq.map { row =>
      val keyBytes = row.getAs[Array[Byte]]("key")
      val valueBytes = row.getAs[Array[Byte]]("value")
      val headersRaw = Option(row.getAs[Seq[Row]]("headers"))
      deserialize(topic, keyBytes, valueBytes, headersRaw, keyDeserializer, valueDeserializer)
    }
  }

  def awaitTermination(): Unit = spark.streams.awaitAnyTermination()

  protected def beforeEach(batchId: Long): Unit = {}
  protected def beforeAll(): Unit = {}
  protected def afterEach(batchId: Long, success: Boolean): Unit = {}
  protected def afterAll(): Unit = {}

  def consumeFromKafka[K, V](
    topic: String,
    keyDeserializer: Deserializer[K],
    valueDeserializer: Deserializer[V],
    f: (Seq[Option[(K, V, Map[String, String])]], Long) => Unit): Unit = {
    configureDeserializers(keyDeserializer, valueDeserializer)
    beforeAll()
    val query = subscribe(topic).writeStream.foreachBatch { (batchDf: DataFrame, batchId: Long ) =>
      var success = true
      beforeEach(batchId)
      try {
        val messages = extractMessageFields(topic, batchDf, keyDeserializer, valueDeserializer)
        f(messages, batchId)
      } catch {
        case e: Exception =>
          success = false
          println(s"[ERROR] Exception during batch $batchId: ${e.getMessage}")
      } finally {
        afterEach(batchId, success)
      }
    }.start()

    sys.addShutdownHook {
      println("Stopping streaming query...")
      try {
        query.stop()
        spark.stop()
        afterAll()
      } catch {
        case e: Exception =>
          println(s"Error during while shutting down: ${e.getMessage}")
      }
    }
  }
}
