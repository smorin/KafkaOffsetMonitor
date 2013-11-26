package com.quantifind.kafka.consumer

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import kafka.message.MessageAndMetadata

class ConsumerTest extends FunSuite with ShouldMatchers {
  test("basic consumer") {
    val consumer = KafkaConsumer(
      zookeeper="localhost:2181",
      group="test_consumer_group",
      topic="test_topic",
      maxThreads = 4,
      workerFactory = () => new PrintWorker()
    )
    //let it read a while ...
    // ... then shut it down
    consumer.shutdown
  }
}

class PrintWorker extends ConsumerWorker[String,String] {
  def handleMessage(msg: MessageAndMetadata[String,String]) {
    if (math.random < 0.005)
      println(msg.message)
  }
}

object ConsumerTest {
  def main(args: Array[String]) {
    val consumer = KafkaConsumer(
      zookeeper="localhost:2181",
      group="test_consumer_group",
      topic="realtime_counters",
      maxThreads = 4,
      workerFactory = () => new PrintWorker()
    )
    //let it read a while ...
    Thread.sleep(10 * 1000)
    // ... then shut it down
    consumer.shutdown

  }
}