package dev.toaster.commons.serialization

import io.confluent.kafka.serializers.{KafkaAvroDeserializer => ConfluentKafkaAvroDeserializer}
import org.apache.kafka.common.serialization.Deserializer

import java.util

class KafkaAvroDeserializer[T] extends Deserializer[T] {

  private val deserializer = new ConfluentKafkaAvroDeserializer()

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    deserializer.configure(configs, isKey)
  }

  override def deserialize(topic: String, data: Array[Byte]): T = {
    if (data == null) null.asInstanceOf[T]
    else deserializer.deserialize(topic, data).asInstanceOf[T]
  }
}
