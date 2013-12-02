package com.quantifind.kafka.consumer

import scala.collection.{Seq, Iterable}
import kafka.consumer.{ConsumerConfig, ConsumerConnector}
import org.I0Itec.zkclient.ZkClient
import kafka.utils.ZKGroupTopicDirs
import kafka.utils.ZkUtils._

/**
 * this is needed as a bridge until https://issues.apache.org/jira/browse/KAFKA-1144 makes it into kafka.  In the
 * meantime, we just do it ourselves
 */
trait OffsetCommitter {
  def commitOffsets(offsets: Iterable[(String, Iterable[PartitionTopicOffset])])

  def commitOffsets(offsets: Seq[PartitionTopicOffset]) {
    commitOffsets(offsets.groupBy{pto => pto.topic})
  }

}

object OffsetCommitter {
  def apply(consumer: ConsumerConnector): OffsetCommitter = {
    //can't use match b/c its a package private class
    val cls = consumer.getClass()
    if (cls.getCanonicalName() == "kafka.consumer.ZookeeperConsumerConnector") {
      //scary reflection code ... but this get us into the internals that we need ...
      val zkField = cls.getField("zkClient")
      zkField.setAccessible(true)
      val zkClient = zkField.get(consumer).asInstanceOf[ZkClient]
      val configField = cls.getField("config")
      configField.setAccessible(true)
      val group = configField.get(consumer).asInstanceOf[ConsumerConfig].groupId
      new ZookeeperOffsetCommitter(zkClient, group)
    } else {
      throw new RuntimeException("sorry, don't know what do with consumers of type " + cls)
    }
  }

}

private[consumer] class ZookeeperOffsetCommitter(val zkClient: ZkClient, val groupId: String) extends OffsetCommitter {
  def commitOffsets(offsets: Iterable[(String, Iterable[PartitionTopicOffset])]) {
    for {
      (topic, infos) <- offsets
      topicDirs = new ZKGroupTopicDirs(groupId, topic)
      info <- infos
    } {
      val newOffset = info.offset
        try {
          val path = topicDirs.consumerOffsetDir + "/" + info.partition
          // we could do a conditional update here, preventing a backwards commit.  However, that won't be possible
          // in kafka 0.8.1 when zookeeper isn't used, so better we don't start relying on it in the meantime
          // see notes here https://issues.apache.org/jira/browse/KAFKA-1144
          updatePersistentPath(zkClient, path,
              newOffset.toString)
        } catch {
          case t: Throwable =>
            // log it and let it go
            warn("exception during commitOffsets",  t)
        }
        debug("Committed offset " + newOffset + " for topic " + info)
    }
  }

}


case class PartitionTopicOffset(topic: String, partition: Int, offset: Long)