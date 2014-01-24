package com.quantifind.kafka

import com.quantifind.utils.UnfilteredWebApp
import unfiltered.filter.Plan
import kafka.utils.{ZKStringSerializer, Logging}
import unfiltered.request.{Seg, GET, Path}
import org.I0Itec.zkclient.ZkClient
import com.quantifind.kafka.OffsetGetter.KafkaInfo
import unfiltered.response.{Ok, ResponseString, JsonContent, Json}
import net.liftweb.json.Serialization.write
import net.liftweb.json.{Serialization, NoTypeHints}

class OWArgs extends OffsetGetterArgs with UnfilteredWebApp.Arguments

/**
 * TODO DOC
 * User: pierre
 * Date: 1/23/14
 */
object OffsetGetterWeb extends UnfilteredWebApp[OWArgs] with Logging {
  def htmlRoot: String = "/offsetapp"


  def withZK[T](args: OWArgs)(f: ZkClient => T): T = {
    var zkClient: ZkClient = null
    try {
      zkClient = new ZkClient(args.zk,
        args.zkSessionTimeout.toMillis.toInt,
        args.zkConnectionTimeout.toMillis.toInt,
        ZKStringSerializer)
      f(zkClient)
    } finally {
      if (zkClient != null)
        zkClient.close()
    }

  }

  def withOG[T](args: OWArgs)(f: OffsetGetter => T): T = withZK(args) {
    zk =>

      var og: OffsetGetter = null
      try {
        og = new OffsetGetter(zk)
        f(og)
      } finally {
        if (og != null) og.close()
      }
  }

  def getInfo(group: String, args: OWArgs): KafkaInfo = withOG(args) {
    _.getInfo(group)
  }

  def getGroups(args:OWArgs) = withOG(args) {
    _.getGroups
  }

  def setup(args: OWArgs): Plan = new Plan {
    implicit val formats = Serialization.formats(NoTypeHints)
    def intent: Plan.Intent = {
      case GET(Path(Seg("group" :: Nil))) =>
        JsonContent ~> ResponseString(write(getGroups(args)))
      case GET(Path(Seg("group" :: group :: Nil))) =>
        val info = getInfo(group, args)
        JsonContent ~> ResponseString(write(info)) ~> Ok
    }
  }
}
