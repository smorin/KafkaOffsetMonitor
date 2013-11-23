package com.quantifind.kafka.consumer

/**
 * Responsible for determining if we're done w/ a "batch" of messages.  Depending on the context, a batch
 * could mean
 *
 * 1) a set of messages which your app must process as a group before its done
 *
 * or
 *
 * 2) a set of messages that are ready to be committed (because your app finishes processing each message one at a time)
 */
trait Batcher {
  //this could get passed the number of msgs & the total size of processed messages if that was useful
  def isBatchDone() : Boolean
  def start()
}

class BatchByTime(val maxNanos: Long) extends Batcher {
  var lastBatchTime: Long = _
  def start() {lastBatchTime = System.nanoTime()}
  def isBatchDone() = {
    val now = System.nanoTime()
    if (now - lastBatchTime > maxNanos) {
      lastBatchTime = now
      true
    } else {
      false
    }
  }
}