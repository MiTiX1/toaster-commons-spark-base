# Spark Base App

A `Scala` library that provides a unified base for Spark applications using Kafka. It includes reusable abstractions and configurations to help you focus on your business logic rather than boilerplate.

## 🚀 Features

- `SparkBase`: A configurable and extendable base class for Apache Spark applications.
- `KafkaProducerClient`: A simple Kafka producer client wrapper to streamline Kafka message publishing.
- _Avro_ / _Proto_ deserialization support with integration with Schema Registry.
- Shared config patterns and utility methods (optional – add more as you expand!).

## How to Use

````scala
object SparkApp extends SparkBase[Key, Value] {
  val keyDeserializer: KafkaAvroDeserializer[Key] =
    new KafkaAvroDeserializer[Key]
  val valueDeserializer: KafkaAvroDeserializer[Value] =
    new KafkaAvroDeserializer[Value]
  implicit val keySerializer: KafkaAvroSerializer[Key] =
    new KafkaAvroSerializer[Key]
  implicit val valueSerializer: KafkaAvroSerializer[Value] =
    new KafkaAvroSerializer[Value]
  val kafkaProducerClient = new KafkaProducerClient[Key, Value]()

  def main(args: Array[String]): Unit = {
    configure(keyDeserializer, valueDeserializer)
    
    consumeFromKafka("input", (messages, batchId) => {
      messages.foreach {
        case Some((key, value, headers)) =>
          println(s"key: $key, value: $value, headers: $headers")
          kafkaProducerClient.send("output", key, value, headers)
        case None => println(s"Error reading values in batch: $batchId")
      }
    })

    consumeFromKafka("output", (messages, batchId) => {
      messages.foreach {
        case Some((key, value, headers)) =>
          println(s"key: $key, value: $value, headers: $headers")
        case None => println(s"Error reading values in batch: $batchId")
      }
    })

    awaitTermination()
  }
}
````

## ⏱️ Features to Come

- Enhanced error logging and metrics integration.