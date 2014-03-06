package com.quantifind.kafka.offsetapp

import java.util.{Timer, TimerTask}

import scala.concurrent.duration._

import com.quantifind.kafka.OffsetGetter.KafkaInfo
import com.quantifind.utils.UnfilteredWebApp
import kafka.utils.{Logging, ZKStringSerializer}
import net.liftweb.json.{CustomSerializer, NoTypeHints, Serialization}
import net.liftweb.json.Serialization.write
import org.I0Itec.zkclient.ZkClient
import unfiltered.filter.Plan
import unfiltered.request.{GET, Path, Seg}
import unfiltered.response.{JsonContent, Ok, ResponseString}
import com.quantifind.kafka.OffsetGetter
import com.quantifind.sumac.validation.Required
import com.twitter.util.Time
import net.liftweb.json.JsonAST.JInt

class OWArgs extends OffsetGetterArgs with UnfilteredWebApp.Arguments {
  @Required
  var retain: FiniteDuration = _

  @Required
  var refresh: FiniteDuration = _

  var dbName: String = "offsetapp"

  lazy val db = new OffsetDB(dbName)
}

/**
 * A webapp to look at consumers managed by kafka and their offsets.
 * User: pierre
 * Date: 1/23/14
 */
object OffsetGetterWeb extends UnfilteredWebApp[OWArgs] with Logging {
  def htmlRoot: String = "/offsetapp"

  val timer = new Timer()

  def writeToDb(args: OWArgs) {
    val groups = getGroups(args)
    groups.foreach {
      g =>
        val inf = getInfo(g, args).offsets.toIndexedSeq
        info(s"inserting ${inf.size}")
        args.db.insetAll(inf)
    }
  }

  def schedule(args: OWArgs) {
    timer.scheduleAtFixedRate(new TimerTask() {
      override def run() {
        writeToDb(args)
      }
    }, 0, args.refresh.toMillis)
    timer.scheduleAtFixedRate(new TimerTask() {
      override def run() {
        args.db.emptyOld(System.currentTimeMillis - args.retain.toMillis)
      }
    }, args.retain.toMillis, args.retain.toMillis)
  }


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

  def getGroups(args: OWArgs) = withOG(args) {
    _.getGroups
  }

  override def afterStop() {
    timer.cancel()
    timer.purge()
  }

  class TimeSerializer extends CustomSerializer[Time](format => (
    {
      case JInt(s)=>
        Time.fromMilliseconds(s.toLong)
    },
    {
      case x: Time =>
        JInt(x.inMilliseconds)
    }
    ))

  override def setup(args: OWArgs): Plan = new Plan {
    implicit val formats = Serialization.formats(NoTypeHints) + new TimeSerializer
    args.db.maybeCreate()
    schedule(args)

    def intent: Plan.Intent = {
      case GET(Path(Seg("group" :: Nil))) =>
        JsonContent ~> ResponseString(write(getGroups(args)))
      case GET(Path(Seg("group" :: group :: Nil))) =>
        val info = getInfo(group, args)
        JsonContent ~> ResponseString(write(info)) ~> Ok
      case GET(Path(Seg("group" :: group :: topic :: Nil))) =>
        val offsets = args.db.offsetHistory(group, topic)
        JsonContent ~> ResponseString(write(offsets)) ~> Ok
    }
  }
}
