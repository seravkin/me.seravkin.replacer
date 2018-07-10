package me.seravkin.replacer.config

/**
  * Application configuration
  */
final case class ReplacerConfiguration(token: String, driver: String, jdbcUrl: String, username: String, password: String)