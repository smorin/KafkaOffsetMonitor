package com.quantifind.kafka.consumer

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 *
 */
class OffsetCommitterTest extends FunSuite with ShouldMatchers {
  test("extract zkClient with reflection") {
    val config = KafkaConsumer.config("localhost:2181", group="blah", consumerTimeout=100)
    val consumer = kafka.consumer.Consumer.create(config)
    val committer = OffsetCommitter(consumer)
    committer.getClass() should be (classOf[ZookeeperOffsetCommitter])
  }
}
