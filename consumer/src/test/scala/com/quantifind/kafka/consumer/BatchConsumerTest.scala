package com.quantifind.kafka.consumer

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class BatchConsumerTest extends FunSuite with ShouldMatchers {
  test("batch consumer"){
    val consumer = KafkaBatchConsumer(
      zookeeper="localhost:2181",
      group="test_consumer_group",
      topic="test_topic",
      maxThreads = 4,
      workerFactory = () => new MessageBufferingWorker[String,String](),
      batchMerger = new PrintBatch
    )
    //let it run awhile ...
    //then shut it down
    consumer.shutdown
  }
}

class PrintBatch extends BatchMerger[Seq[String]] {
  def handleBatch(batch: Iterator[Seq[String]]) {
    //in a real use case, you'd store your buffern to a datastore here, eg. a batch insert to a DB
    batch.flatten.foreach{println}
  }
}