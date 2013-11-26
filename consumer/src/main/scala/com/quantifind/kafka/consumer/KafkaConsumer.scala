package com.quantifind.kafka.consumer

import java.util.Properties
import java.util.concurrent.Executors

import scala.collection._
import scala.collection.JavaConverters._
import scala.concurrent.duration._

import org.I0Itec.zkclient.ZkClient

import kafka.utils.ZkUtils
import kafka.utils.ZKStringSerializer
import kafka.serializer.{StringDecoder, Decoder}
import kafka.consumer._
import kafka.message.MessageAndMetadata

abstract class ConsumerTemplate[K,V,W,P <: Runnable](
    val consumerConfig: ConsumerConfig,
    val topicsToThreadsAndWorkers: Map[String,(Int, () => W)],
    val batchTime: Duration,
    val keyDecoder: Decoder[K],
    val valueDecoder: Decoder[V]
) {

  require(consumerConfig.consumerTimeoutMs > 0, "you must specify consumer.timeout.ms > 0")
  require(!consumerConfig.autoCommitEnable, "you must specify auto.commit.enable = false")



  private[kafka] val consumer = kafka.consumer.Consumer.create(consumerConfig)
  private val topicIterators : Map[String, List[ConsumerIterator[K, V]]] =  {
    //does kafka auto-correct if I provide too many threads per topic?  the docs make me think the answer is no,
    // which is why I do this check.  but seems a little crazy
    val topics = topicsToThreadsAndWorkers.keys.toSeq
    val zookeeper = consumerConfig.props.getString("zookeeper.connect")
    val zkClient = new ZkClient(zookeeper, 30000, 30000, ZKStringSerializer)
    val topicPartitions = ZkUtils.getPartitionsForTopics(zkClient, topics)
    zkClient.close()

    consumer.createMessageStreams[K,V](
      topicCountMap = topics.map{t => t -> math.min(topicsToThreadsAndWorkers(t)._1, topicPartitions(t).size)}(breakOut),
      keyDecoder = keyDecoder,
      valueDecoder = valueDecoder
    ).map{case(k,v) => k -> v.map{_.iterator()}}
  }

  protected def makeWorkerRunnable(w: W, itr: Iterator[MessageAndMetadata[K,V]]): P
  protected def commitFinalOffsets() : Unit

  private val topicToWorkers : Map[String, List[P]] = {
    topicIterators.map{case(topic, itrs) =>
      val workerFactory = topicsToThreadsAndWorkers(topic)._2
      val workers = itrs.map{itr =>
        makeWorkerRunnable(workerFactory(), itr)
      }
      topic -> workers
    }
  }

  private[kafka] val workers = topicToWorkers.values.flatten
  val nThreads = workers.size

  private val pool = Executors.newFixedThreadPool(nThreads)
  workers.foreach{w => pool.submit(w)}

  def shutdown() {
    pool.shutdownNow()
    commitFinalOffsets()
    consumer.shutdown()
  }

}

class KafkaConsumer[K,V](
  consumerConfig: ConsumerConfig,
  topicsToThreadsAndWorkers: Map[String,(Int, () => ConsumerWorker[K,V])],
  batchTime: Duration,
  keyDecoder: Decoder[K],
  valueDecoder: Decoder[V]
) extends ConsumerTemplate[K,V,ConsumerWorker[K,V], KafkaProcessor[K,V]](
  consumerConfig, topicsToThreadsAndWorkers, batchTime, keyDecoder, valueDecoder
) {
  override
  def makeWorkerRunnable(worker: ConsumerWorker[K,V], itr: Iterator[MessageAndMetadata[K,V]]) = {
    new KafkaProcessor(
      consumer,
      worker,
      itr,
      batchTime.toNanos
    )
  }

  override
  def commitFinalOffsets() {
    workers.foreach{w => w.doCommit()}
  }

}


object KafkaConsumer {

  def apply(
    zookeeper: String,
    group: String,
    topic: String,
    maxThreads: Int,
    workerFactory: () => ConsumerWorker[String,String],
    consumerTimeout: Long = 100,
    batchTime: Duration =  1.minute
  ) : KafkaConsumer[String,String] = {
    new KafkaConsumer(
      config(zookeeper, group, consumerTimeout),
      Map(topic -> (maxThreads, workerFactory)),
      batchTime,
      new StringDecoder(),
      new StringDecoder()
    )
  }

  def config(zookeeper: String, group: String, consumerTimeout: Long) = {
    val props = new Properties()
    props.putAll(Map(
      "zookeeper.connect" -> zookeeper,
      "group.id" -> group,
      "zookeeper.session.timeout.ms" -> "400",
      "zookeeper.sync.time.ms" -> "200",
      "auto.commit.enable" -> "false",
      "auto.offset.reset" -> "smallest",
      "consumer.timeout.ms" -> consumerTimeout.toString
    ).asJava)
    new ConsumerConfig(props)
  }

}

private[kafka] class KafkaProcessor[K,V](
    val consumer: ConsumerConnector,
    val worker: ConsumerWorker[K,V],
    val itr: Iterator[MessageAndMetadata[K,V]],
    val batchTimeNanos: Long
) extends Runnable with PositionTracker {

  var lastBatchTime : Long = _
  def run() {
    try {
      lastBatchTime = System.nanoTime()
      while(true) {
        try {
          while(itr.hasNext) {
            //note that calling next() will update the consumers internal notion of offsets, but we don't care.
            // we only use offsets from the messages themselves
            val next = itr.next()
            worker.handleMessage(next)
            updatePosition(next)
            maybeCommit()
          }
        } catch {
          case cto: ConsumerTimeoutException =>
            maybeCommit()
        }
      }
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
    }
  }

  def maybeCommit() {
    val now = System.nanoTime()
    if (now - lastBatchTime > batchTimeNanos) {
      doCommit()
      lastBatchTime = now
    }
  }

  def doCommit() {
    consumer.commitOffsets(positions, preventBackwardsCommit = true)
  }
}

private[kafka] trait PositionTracker {
  private[kafka] val _positions = mutable.Map[TopicPartition,Long]()
  def updatePosition(msg: MessageAndMetadata[_,_]) {
    val k = TopicPartition(msg.topic, msg.partition)
    _positions(k) = msg.offset
  }
  def positions: Seq[PartitionTopicOffset] = {
    //the offset that gets committed is the next offset TO BE READ, so add one
    _positions.map{case(tp, offset) => PartitionTopicOffset(tp.topic, tp.partition, offset + 1)}(breakOut)
  }
}
