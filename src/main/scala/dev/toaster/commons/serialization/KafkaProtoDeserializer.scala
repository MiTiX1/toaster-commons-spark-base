package dev.toaster.commons.serialization

import org.apache.kafka.common.serialization.Deserializer
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import java.util
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

class KafkaProtoDeserializer[T <: GeneratedMessage : TypeTag : ClassTag] extends Deserializer[T] {

  private val companion: GeneratedMessageCompanion[T] = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val companionSymbol = typeOf[T].typeSymbol.companion.asModule
    val companionMirror = mirror.reflectModule(companionSymbol)
    companionMirror.instance.asInstanceOf[GeneratedMessageCompanion[T]]
  }

  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = ()

  override def deserialize(topic: String, data: Array[Byte]): T = {
    if (data == null) null.asInstanceOf[T]
    else
      try {
        companion.parseFrom(data)
      } catch {
        case e: Exception =>
          throw new RuntimeException(s"Error deserializing message for topic $topic", e)
      }
  }

  override def close(): Unit = ()
}
