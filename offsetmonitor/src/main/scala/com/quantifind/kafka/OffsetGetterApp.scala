package com.quantifind.kafka

import com.quantifind.sumac.{FieldArgs, ArgMain}
import com.quantifind.sumac.validation.Required
import kafka.utils.ZKStringSerializer
import org.I0Itec.zkclient.ZkClient
import scala.concurrent.duration._

class OffsetGetterArgsWGT extends OffsetGetterArgs {
  @Required
  var group: String = _

  var topics: Seq[String] = Seq()
}

class OffsetGetterArgs extends FieldArgs {
  @Required
  var zk: String = _



  var zkSessionTimeout: Duration = 30 seconds
  var zkConnectionTimeout: Duration = 30 seconds

}

/**
 * TODO DOC
 * User: pierre
 * Date: 1/22/14
 */
object OffsetGetterApp extends ArgMain[OffsetGetterArgsWGT] {


  def main(args: OffsetGetterArgsWGT) {
    var zkClient: ZkClient = null
    var og: OffsetGetter = null
    try {
      zkClient = new ZkClient(args.zk,
        args.zkSessionTimeout.toMillis.toInt,
        args.zkConnectionTimeout.toMillis.toInt,
        ZKStringSerializer)
      og = new OffsetGetter(zkClient)
      val i = og.getInfo(args.group, args.topics)

      if (i.offsets.nonEmpty) {
        println("%-15s\t%-40s\t%-3s\t%-15s\t%-15s\t%-15s\t%s".format("Group", "Topic", "Pid", "Offset", "logSize", "Lag", "Owner"))
        i.offsets.foreach {
          info =>
            println("%-15s\t%-40s\t%-3s\t%-15s\t%-15s\t%-15s\t%s".format(info.group, info.topic, info.partition, info.offset, info.logSize, info.lag,
              info.owner.getOrElse("none")))
        }
        println()
        println("Brokers")
        i.brokers.foreach {
          b =>
            println(s"${b.id}\t${b.host}:${b.port}")
        }
      } else {
        System.err.println(s"no topics for group ${args.group}")
      }

    }
    finally {
      if (og != null) og.close()
      if (zkClient != null)
        zkClient.close()
    }
  }


}

