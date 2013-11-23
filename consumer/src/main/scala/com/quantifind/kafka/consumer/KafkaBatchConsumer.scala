package com.quantifind.kafka.consumer

import java.util.concurrent.{BlockingQueue, ArrayBlockingQueue}

import scala.collection._
import scala.collection.JavaConverters._
import scala.concurrent.duration._


import kafka.message.MessageAndMetadata
import kafka.consumer.{ConsumerConfig, ConsumerTimeoutException, ConsumerConnector, PartitionTopicOffset}
import kafka.serializer.{StringDecoder, Decoder}

/**
 * this is a kafka batch reader that guarantees messages are processed at least once by your application.
 *
 * Bigger batches mean fewer updates to zookeeper, and higher throughput.  But it also means that your app has to
 * store more in memory, you'll go back further on restart, etc.
 *
 * Why at least once, and not exactly once?  Two reasons:
 *
 * 1) a partition could get "rebalanced" to another thread in the middle of processing.  Some messages will get
 * read by multiple threads, but we don't really know exactly which ones.
 *
 * 2) Its possible this dies after your batch is processed, but before
 * the offsets are stored in zookeeper.  Then, on restart, you'll re-read the entire last batch.  (This is
 * another argument for small batches.)
 *
 */
class KafkaBatchConsumer[K,V,T](
  consumerConfig: ConsumerConfig,
  topicsToThreadsAndWorkers: Map[String,(Int, () => BatchConsumerWorker[K,V,T])],
  batchMerger: BatchMerger[T],
  batcher: Batcher,
  keyDecoder: Decoder[K],
  valueDecoder: Decoder[V]
  ) extends ConsumerTemplate[K,V,BatchConsumerWorker[K,V,T], KafkaBatchProcessor[K,V,T]](
  consumerConfig, topicsToThreadsAndWorkers, batcher, keyDecoder, valueDecoder
) {

  val batchQueue = new ArrayBlockingQueue[(Seq[PartitionTopicOffset],T)](nThreads)

  override
  def makeWorkerRunnable(worker: BatchConsumerWorker[K,V,T], itr: Iterator[MessageAndMetadata[K,V]]) = {
    new KafkaBatchProcessor(
      consumer,
      worker,
      itr,
      batcher,
      batchQueue
    )
  }

  def processBatchNow() = {
    val jBatchesToProcess = new java.util.ArrayList[(Seq[PartitionTopicOffset],T)]()
    batchQueue.drainTo(jBatchesToProcess)
    val batchesToProcess = jBatchesToProcess.asScala
    val offsetsToCommit = mergeOffsets(batchesToProcess.map{_._1}.flatten)

    //now the final step of your app to process the data.
    batchMerger.handleBatch(batchesToProcess.iterator.map{_._2})

    //TODO if the app dies right here, then the batch has been fully processd, but not committed, so
    // we'll process the batch again next time.  We can't really completely prevent that, but we should at
    // least try on a "graceful" shutdown -- if the user tries to shutdown in the middle here, wait till
    // commit happens, then quit.

    //and now that your app is through with processing the batch, we can commit the updates
    consumer.commitOffsets(offsetsToCommit)
  }

  def mergeOffsets(offsets: Seq[PartitionTopicOffset]): Map[String, Iterable[PartitionTopicOffset]] = {
    offsets.groupBy{pto => pto.topic}.map{case(topic, partitionsAndOffsets) =>
      val maxPerTopic: Iterable[PartitionTopicOffset] = partitionsAndOffsets.groupBy{_.partition}.map{ case(partition, offsets) =>
        //for each partition, we just want the max offset.
        offsets.maxBy{_.offset}
      }
      topic -> maxPerTopic
    }
  }
}



object KafkaBatchConsumer {

  def apply[T](
    zookeeper: String,
    group: String,
    topic: String,
    maxThreads: Int,
    workerFactory: () => BatchConsumerWorker[String,String,T],
    batchMerger: BatchMerger[T],
    consumerTimeout: Long = 100,
    batcher: Batcher = new BatchByTime(1.minute.toNanos)
  ) : KafkaBatchConsumer[String,String,T] = {
    new KafkaBatchConsumer(
      KafkaConsumer.config(zookeeper, group, consumerTimeout),
      Map(topic -> (maxThreads, workerFactory)),
      batchMerger,
      batcher,
      new StringDecoder(),
      new StringDecoder()
    )
  }
}

private[kafka] class KafkaBatchProcessor[K,V,T](
    val consumer: ConsumerConnector,
    val worker: BatchConsumerWorker[K,V,T],
    val itr: Iterator[MessageAndMetadata[K,V]],
    val batcher: Batcher,
    val batchQueue: BlockingQueue[(Seq[PartitionTopicOffset],T)]
) extends Runnable with PositionTracker {

  def run() {

    while (true) {
      try {
        while(itr.hasNext) {
          //note that calling next() will update the consumers internal notion of offsets, but we don't care.
          // we only use offsets from the messages themselves
          val next = itr.next()
          worker.addMessageToBatch(next)
          updatePosition(next)
          if (batcher.isBatchDone()) {
            batchQueue.put(getBatchAndOffsets())
          }
        }
      } catch {
        case cto: ConsumerTimeoutException =>
          if (batcher.isBatchDone()) {
            batchQueue.put(getBatchAndOffsets())
          }
      }
    }
  }

  private[kafka] def getBatchAndOffsets() : (Seq[PartitionTopicOffset], T) = {
    val ptos = positions
    val b = worker.getBatch
    (ptos, b)
  }
}


private[kafka] case class TopicPartition(topic:String, partition: Int)