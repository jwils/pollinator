Pollinator
==========


Introduction
------------
Pollinator is a real-time push server designed to handle a large number of clients. Pollinator is made of three
components: the publisher, the server, and the client. The publisher pushes messages to redis with a message
timeout. The server is designed to handle all client messages and push new messages to active clients as
messages are enqueued. The client subscribes to a server with an id and receives messages and handles it in some way.

Pollinator is designed to be fault tolerant, require minimal communication between components, and scale to hundreds of
thousands of clients.

Push Messages
-------------
Pollinators push messages contain 4 properties: id, event, data, and appId. The id is a unique monotonically increasing
long per client. When the first push message is enqueued for a client a counter will be initialized to 1. Every time a
new message is enqueued for a client the counter will be incremented and the value will be used for the id field.
The event field stores the type of action the push message represents. For example a application may have a 'sync' event.
Events can also store information on how messages should be grouped.

There are three types of events:

1. non persistent - These messages contain only an eventName and are sent once if the device is currently online.
They are not retried on failure. These type of messages are good for operations that are only useful with instant feedback.

1. Groupable - Groupable messages do not require a unique delivery. When enqueued a message of that eventName
will reach the client. If multiple messages with the same groupable event name are enqueued at least one will make it to
the client. Groupable messages do not support data. These type of messages a good for sync notifications.

1. Persistent - Persistent messages support all fields and are not grouped. Every message enqueued is guaranteed to reach
the client.

The data field is used to store message data as a string.

The appId field is used when treating pollinator as a service for many client apps. The appId determines which app
the notification should be sent to.

Redis
------
Redis is used as the persistence layer of the application. It is also used for the limited communication between the
publishers and the servers. Pollinator is designed to shard across any number of redis instances. Each device is assigned
randomly to a redis shard based on the hash of its device id. If a redis instance goes down, Pollinator will attempt to
reconnect but all devices associated with that instance will not receive messages till the instance is back online.
Redis master-slave configurations should be examined for higher reliability.

The Publisher
-------------
The Publisher is designed to enqueue push messages in redis and is made up of two components. The RedisPushPublisher
accepts incoming push messages and adds them to a queue based on the id of the device. Each queue is periodically flushed
by a MessagePoolQueuePublisher which will encode the message in redis. After each iteration the MessagePoolQueuePublisher
will iterate through each server determining the devices that are online. If the device a message targets is online for
a given server a event is enqueued in a redis list which notifies the server to check the device messages.


The Server
------------
A server accepts incoming client socket connections. It keeps track of all active clients connected to it, and periodically
sends keep alive messages to ensure the connection is still active. The server has an event thread listening on a list
across each redis shard. When an event comes in it will contain a deviceId, if the device is connected to the current server
the server will remove all entries from the device message queue add them to a pending message queue and send them to
the client. When a client receives a message it will send an ack back to the server. At this time the message is removed
from the pending message queue. When a device first reconnects the pending message queue is flushed of messages.

The Client
----------
The Client opens a connection to the server. It receives messages and sends back acks. When a message comes in the id is
compared to the last id seen. If the id is less than or equal to the last id seen the message is discarded, otherwise it
is processed.


Running an Example
-----------------
Prereqs:

To compile you'll need to have maven installed

To run you'll need to have redis installed and running on the default port (6379).


Compile:

Running `mvn install` in the root directory will compile all modules. (A precompiled jar is located in the root directory.)

The example module contains a version of each of the three components that can easily be run from the command line.

Either use the precompiled jar or the jar located in examples/target/ after you compile.

To Run server:
`java -cp pollinator-example-0.1-jar-with-dependencies.jar com.clover.push.ExamplePublisher`

To run the client:
`java -cp pollinator-example-0.1-jar-with-dependencies.jar com.clover.push.ExampleClient [clientid]`

To run the publisher:
`java -cp pollinator-example-0.1-jar-with-dependencies.jar com.clover.push.ExamplePublisher`

The publisher will prompt for the data to send a push message. If you use the same client as your running example client
the message should print out in the terminal for the client.



Future Work
-----------
This is definitely still a work in progress. Here are some things that I want to work on still:

1. Exception handling needs a bit for work. For example when you shut down the
server while the client is still running you get an ugly error message.
1. A reliable client that reconnects when it gets disconnected.
1. Cleaner configuration and customization of properties.
1. Real tests.

