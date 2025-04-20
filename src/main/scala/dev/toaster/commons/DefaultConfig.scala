package dev.toaster.commons

import com.typesafe.config.{Config, ConfigFactory}

object DefaultConfig {
  lazy val config: Config = ConfigFactory
    .load("application.conf")
    .getConfig("commons")

  lazy val sparkConfig = config.getConfig("spark")
  lazy val appName = sparkConfig.getString("app-name")
  lazy val master = sparkConfig.getString("master")
  lazy val logLevel = sparkConfig.getString("log-level")
  lazy val startingOffsets = sparkConfig.getString("startingOffsets")
  lazy val subscribeTopic = sparkConfig.getString("subscribeTopic")
  lazy val includeHeaders = sparkConfig.getString("includeHeaders")

  lazy val kafkaConfig = config.getConfig("kafka")
  lazy val bootstrapServers = kafkaConfig.getString("bootstrap.servers")
  lazy val acks = kafkaConfig.getString("acks")
  lazy val retries = kafkaConfig.getString("retries")
  lazy val schemaRegistryUrl = kafkaConfig.getString("schema.registry.url")
  lazy val autoRegisterSchemas = kafkaConfig.getString("auto.register.schemas")
  lazy val specificAvroReader = kafkaConfig.getString("specific.avro.reader")

}
