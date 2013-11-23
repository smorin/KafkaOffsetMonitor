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

trait BatchMerger[T] {
  def handleBatch(batch: Iterator[T]) : Unit
}
