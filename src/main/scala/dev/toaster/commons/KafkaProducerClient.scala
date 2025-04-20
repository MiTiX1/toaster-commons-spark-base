package dev.toaster.commons

import dev.toaster.commons.DefaultConfig.{acks, autoRegisterSchemas, bootstrapServers, retries, schemaRegistryUrl, specificAvroReader}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serializer

import java.nio.charset.StandardCharsets
import java.util.Properties

class KafkaProducerClient[K, V]()(
  implicit val keySerializer: Serializer[K],
  implicit val valueSerializer: Serializer[V]
) {

  private val props = new Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("acks", acks)
  props.put("retries", retries)
  props.put("schema.registry.url", schemaRegistryUrl)
  props.put("auto.register.schemas", autoRegisterSchemas)
  props.put("specific.avro.reader", specificAvroReader)
  props.put("key.serializer", keySerializer.getClass.getName)
  props.put("value.serializer", valueSerializer.getClass.getName)

  private val producer = new KafkaProducer[K, V](props)

  keySerializer.configure(props.asInstanceOf[java.util.Map[String, _]], true)
  valueSerializer.configure(props.asInstanceOf[java.util.Map[String, _]], false)

  def send(
            topic: String,
            key: K,
            value: V,
            headers: Map[String, String] = Map.empty
          ): Unit = {
    val kafkaHeaders = new RecordHeaders()
    headers.foreach { case (k, v) =>
      kafkaHeaders.add(k, v.getBytes(StandardCharsets.UTF_8))
    }
    val record = new ProducerRecord[K, V](topic, null, key, value, kafkaHeaders)
    producer.send(record)
  }

  def send(
            topic: String,
            key: K,
            value: V,
          ): Unit = {
    val record = new ProducerRecord[K, V](topic, null, key, value)
    producer.send(record)
  }

  def send(
            topic: String,
            value: V,
          ): Unit = {
    val record = new ProducerRecord[K, V](topic, value)
    producer.send(record)
  }

  def close(): Unit = {
    println("closing kafka producer...")
    producer.close()
  }

  sys.addShutdownHook(close())
}
