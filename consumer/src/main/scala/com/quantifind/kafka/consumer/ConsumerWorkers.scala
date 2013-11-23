package com.quantifind.kafka.consumer

import kafka.message.MessageAndMetadata

trait ConsumerWorker[K,V] {
  def handleMessage(msg: MessageAndMetadata[K,V]): Unit
}

object ConsumerWorker {
  def apply[K,V](f: MessageAndMetadata[K,V] => Unit) = {
    new ConsumerWorker[K,V] {
      def handleMessage(msg: MessageAndMetadata[K,V]) {f(msg)}
    }
  }
}

trait BatchConsumerWorker[K,V,T] {
  def addMessageToBatch(msg: MessageAndMetadata[K,V]) : Unit
  def getBatch() : T
}

class MessageBufferingWorker[K,V] extends BatchConsumerWorker[K,V,Seq[V]] {
  var buffer = Vector[V]()
  def addMessageToBatch(msg: MessageAndMetadata[K,V]) {buffer :+= msg.message}
  def getBatch() = {
    //NOTE: addMessageToBatch CAN NOT get called at the same time as this, so this is safe
    val b = buffer
    buffer = Vector[V]()
    b
  }
}


trait BatchMerger[T] {
  def handleBatch(batch: Iterator[T]) : Unit
}
