# Spark Base App

A `Scala` library that provides a unified base for Spark applications using Kafka. It includes reusable abstractions and configurations to help you focus on your business logic rather than boilerplate.

## 🚀 Features

- `SparkBase`: A configurable and extendable base class for Apache Spark applications.
- `KafkaProducerClient`: A simple Kafka producer client wrapper to streamline Kafka message publishing.
- _Avro_ deserialization support with integration with Schema Registry.
- Shared config patterns and utility methods (optional – add more as you expand!).

## How to Use

````scala
object App extends SparkBase[Key, Person] {
  val keyDeserializer: KafkaAvroDeserializer[Key] =
    new KafkaAvroDeserializer[Key]
  val valueDeserializer: KafkaAvroDeserializer[Person] =
    new KafkaAvroDeserializer[Person]
  implicit val keySerializer: KafkaAvroSerializer[Key] =
    new KafkaAvroSerializer[Key]
  implicit val valueSerializer: KafkaAvroSerializer[Person] =
    new KafkaAvroSerializer[Person]
  val kafkaProducerClient = new KafkaProducerClient[Key, Person]()

  def main(args: Array[String]): Unit = {
    configure(keyDeserializer, valueDeserializer)
    consumeFromKafka { case (messages, batchId) =>
      messages.foreach {
        case Some((key, value, headers)) =>
          println(s"key: $key, value: $value, headers: $headers")
          kafkaProducerClient.send("output", key, value, headers)
        case None =>
          println(s"Error reading values in batch: $batchId")
      }
    }
  }
}
````

## ⏱️ Features to Come

- More deserialization protocols support.
- Enhanced error logging and metrics integration.