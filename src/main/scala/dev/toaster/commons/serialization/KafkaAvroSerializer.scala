package dev.toaster.commons.serialization

import io.confluent.kafka.serializers.{KafkaAvroSerializer => ConfluentKafkaAvroSerializer}
import org.apache.kafka.common.serialization.Serializer

import java.util

class KafkaAvroSerializer[T] extends Serializer[T] {

  private val serializer: ConfluentKafkaAvroSerializer = new ConfluentKafkaAvroSerializer()

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    serializer.configure(configs, isKey)
  }

  override def serialize(topic: String, data: T): Array[Byte] = {
    if (data == null) null
    else serializer.serialize(topic, data)
  }
}
