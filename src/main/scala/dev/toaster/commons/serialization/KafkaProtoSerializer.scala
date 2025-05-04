package dev.toaster.commons.serialization

import org.apache.kafka.common.serialization.Serializer
import scalapb.GeneratedMessage

import java.util

class KafkaProtoSerializer[T <: GeneratedMessage] extends Serializer[T] {

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  override def serialize(topic: String, data: T): Array[Byte] = {
    if (data == null) null
    else
      try {
        data.toByteArray
      } catch {
        case e: Exception =>
          throw new RuntimeException(s"Error serializing message for topic $topic", e)
      }
  }

  override def close(): Unit = ()
}
