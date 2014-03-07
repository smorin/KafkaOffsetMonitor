Kafka Offset Monitor
===========

This is an app to monitor your kafka consumers and their position (offset) in the queue.

You can see the current consumer groups, for each group the topics that they are consuming and the position of the group in each topic queue. This is useful to understand how quick you are consuming from a queue and how fast the queue is growing. It allows for debuging kafka producers and consumers or just to have an idea of what is going on in  your system.

The app keeps an history of queue position and lag of the consumers so you can have an overview of what has happened in the last days.

Here are a few screenshots:

List of Consumer Groups
-----------------------

![Consumer Groups](http://quantifind.github.io/KafkaOffsetMonitor/img/groups.png)

List of Topics for a Group
--------------------------

![Topic List](http://quantifind.github.io/KafkaOffsetMonitor/img/topics.png)

History of Topic position
-------------------------

![Position Graph](http://quantifind.github.io/KafkaOffsetMonitor/img/graph.png)

Running It
===========

If you do not want to build it manually, just download the [current jar](http://quantifind.github.io/KafkaOffsetMonitor/dist/KafkaOffsetMonitor-assembly-0.1.0-SNAPSHOT.jar).

This is a small webapp, you can run it locally or on a server, as long as you have access to the ZooKeeper nodes controlling kafka.

```
java -cp KafkaOffsetMonitor-assembly-0.1.0-SNAPSHOT.jar \
     com.quantifind.kafka.offsetapp.OffsetGetterWeb \
     --zk zk-server1,zk-server2 \
     --port 8080 \
     --refresh 10.seconds \
     --retain 2.days
```

The arguments are:

- **zk** the ZooKeeper hosts
- **port** on what port will the app be available
- **refresh** how often should the app refresh and store a point in the DB
- **retain** how long should points be kept in the DB
- **dbName** where to store the history (default 'offsetapp')
