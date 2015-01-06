Pollinator
==========

Introduction
------------
Pollinator is a real-time push server designed to handle a large number of clients. Pollinator is made of four
components: the publisher, the server, and the client. The publisher pushes messages to redis with a message
timeout. The server is designed to handle all client messages and push new messages to active clients as
messages are enqueued. When a client first connects, the server is responsible for sending all pending
messages. The client subscribes to a server with an id. When messages are sent to the client it is responsible

to be fault tolerant. Multiple server instances can be run simultaneously without direct communication.