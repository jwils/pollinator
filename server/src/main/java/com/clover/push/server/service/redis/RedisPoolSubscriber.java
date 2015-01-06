package com.clover.push.server.service.redis;

import com.clover.push.server.client.PushClientListener;
import com.clover.push.message.AckMessage;
import com.clover.push.message.DefaultPushMessage;
import com.clover.push.message.Event;
import com.clover.push.message.PushMessage;
import com.clover.push.redis.RedisPushUtils;
import com.clover.push.redis.RedisTask;
import com.clover.push.PushMessageSubscriber;


import com.clover.push.util.Ids;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;


/**
 * User: josh
 * Date: 1/6/14
 */
class RedisPoolSubscriber implements Runnable, PushMessageSubscriber {
    private static final int MAX_WAIT_FOR_NEW_MESSAGE_SEC = 5;
    private static final int MAX_BULK_MESSAGE_FOR_CLIENT = 100;

    private static final Logger logger = LoggerFactory.getLogger(RedisPoolSubscriber.class);

    private final JedisPool pool;
    private final String subscriberHash;
    private final String subscriberName;
    private final int poolId;
    private final long redisReconnectDelayMs = 10000;

    private volatile boolean stopped = true;
    private Queue<RedisTask> redisTaskQueue;
    private Map<String, PushClientListener> clients;
    private Thread thread;

    public RedisPoolSubscriber(JedisPool pool, int poolId) {
        this.subscriberHash = Ids.nextBase32Id();
        this.subscriberName = RedisPushUtils.getSubscriberName(subscriberHash, Integer.toString(poolId));
        this.pool = pool;
        this.poolId = poolId;
        this.clients = new ConcurrentHashMap<String, PushClientListener>();
        this.redisTaskQueue = new ConcurrentLinkedQueue<RedisTask>();
    }

    @Override
    public void start() {
        if (!stopped) {
            return;
        }

        thread = new Thread(this, "redis-push-pool-" + poolId + "-hash-" + subscriberHash);
        thread.start();
    }

    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * Until stopped attempt run loop and if disconnected wait for 10000 ms
     * before reconnecting.
     */
    @Override
    public void run() {
        logger.info("Starting pool thread.");

        stopped = false;
        while (!stopped) {
            Jedis conn = null;
            try {
                conn = openConnection();
                //Run continuous loop until disconnected.
                runLoop(conn);
            } catch (JedisConnectionException e) {
                logJedisDisconnect(e);
            } catch (Exception e) {
                logJedisDisconnectUnknownError(e);
            } finally {
                closeConnection(conn);
                logger.info("Shutting down pool " + subscriberHash);
            }
            try {
                Thread.sleep(redisReconnectDelayMs);
            } catch (InterruptedException e) {
                logger.info("interrupted while sleeping after error, continuing");
            }
        }
        thread = null;
        redisTaskQueue = null;
    }

    public boolean isRunning() {
        return !stopped;
    }

    @Override
    public void registerClient(final PushClientListener client) {
        logger.debug("Adding device[" + client.id() + "] to queue");
        PushClientListener oldClient = clients.put(client.id(), client);
        if (oldClient != null && oldClient != client) {
            logger.info("Device[" + client.id() + "] is already connected. Overwriting.");
            oldClient.disconnect();
        }
        enqueueTask(new AddClientTask(client.id()));
    }

    @Override
    public void removeClient(final String clientId) {
        logger.debug("Removing device[" + clientId + "] from queue");
        PushClientListener client = clients.remove(clientId);
        if (client != null) {
            client.disconnect();
            enqueueTask(new RemoveClientTask(clientId));
        }
    }

    private void runLoop(Jedis conn) throws Exception {
        String clientSet = RedisPushUtils.getActiveClientSetName(subscriberName);
        String clientActionList = RedisPushUtils.getPoolSubscriberActionQueueName(subscriberName);
        //if we disconnect make sure all the clients are added back to the key
        String[] clientsKeys = clients.keySet().toArray(new String[clients.keySet().size()]);
        if (clientsKeys.length > 0) {
            conn.sadd(clientSet, clientsKeys);
        }
        while (!stopped) {
            //EXPIRE this key after 10 minutes.
            // If it isn't active in that time frame we can remove it.
            conn.expire(clientSet, 60 * 10);
            conn.expire(clientActionList, 60 * 60); //1 hour expiration.

            runTasks(conn);
            waitForMessageAndSendToClient(conn);

        }
        logger.info("Disconnecting push pool subscriber");
        //Empty task queue after stopped.
        enqueueTask(new RemoveClientTask("CONNECTED"));
        for (String client : clients.keySet()) {
            enqueueTask(new RemoveClientTask(client));
        }

        runTasks(conn);
        for (String client : clients.keySet()) {
            clients.get(client).shutdown();
        }
        logger.info("Finished disconnecting push pool subscriber");
    }

    private Jedis openConnection() {
        Jedis conn = pool.getResource();
        //Add queue name to global subscriber list.
        conn.sadd(RedisPushUtils.SUBSCRIBER_QUEUE_LIST, subscriberName);
        conn.persist(RedisPushUtils.SUBSCRIBER_QUEUE_LIST); //ENSURE subscriberQueue list persists
        enqueueTask(new AddClientTask("CONNECTED"));
        return conn;
    }

    private void closeConnection(Jedis conn) {
        if (conn.isConnected()) {
            try {
                conn.srem(RedisPushUtils.SUBSCRIBER_QUEUE_LIST, subscriberName);
            } catch (JedisConnectionException e) {
                logger.warn("Failed to disconnect gracefully", e);
            }
        }
        pool.returnResource(conn);
    }

    public boolean enqueueTask(RedisTask task) {
        return redisTaskQueue.offer(task);
    }

    private List<PushMessage> getMessagesForClient(Jedis conn, String clientId) {
        String clientQueueName = RedisPushUtils.getClientQueueName(clientId);
        String clientPendingQueueName = RedisPushUtils.getClientPendingQueueName(clientId);

        List<PushMessage> messages = new ArrayList<PushMessage>();

        String messageId = conn.rpoplpush(clientQueueName, clientPendingQueueName);
        while (messageId != null && messages.size() < MAX_BULK_MESSAGE_FOR_CLIENT) {
            if (RedisPushUtils.isInteger(messageId)) {
                PushMessage msg = getMessage(conn, clientId, messageId);
                //TODO we can check two messages with same content and different id. Collapse?
                if (msg != null && !messages.contains(msg)) {
                    messages.add(msg);
                }
            } else {
                logger.debug("building message from event: " + messageId);
                messages.add(new DefaultPushMessage(new Event(messageId, true), null, null));
            }
            messageId = conn.rpoplpush(clientQueueName, clientPendingQueueName);
        }

        return messages;
    }

    private List<PushMessage> getPendingMessagesForClient(Jedis conn, String clientId) {
        String clientPendingQueueName = RedisPushUtils.getClientPendingQueueName(clientId);

        List<PushMessage> messages = new ArrayList<PushMessage>();

        List<String> messageIds = conn.lrange(clientPendingQueueName, 0, -1);
        //For now we will only retry once. We need to figure out clients that are failing to ack
        conn.del(clientPendingQueueName);

        messageIds = Lists.reverse(messageIds);


        logger.info("Client[" + clientId + "] has " + messageIds.size() + " pending messages.");

        for (String messageId : messageIds) {
            if (RedisPushUtils.isInteger(messageId)) {
                PushMessage msg = getMessage(conn, clientId, messageId);
                if (msg == null) {
                    //Remove messages that are no longer in redis.
                    conn.lrem(clientPendingQueueName, 0, messageId);
                }
                //TODO we can check two messages with same content and different id. Collapse?
                if (msg != null) {
                    messages.add(msg);
                }
            } else {
                logger.debug("building message from event: " + messageId);
                messages.add(new DefaultPushMessage(new Event(messageId, true), null, null));
            }
        }
        return messages;
    }

    @Override
    public void ackMessage(final String clientId, final AckMessage message) {
        enqueueTask(new RedisTask() {
            @Override
            public void runTask(Jedis conn) {
                ackMessage(conn, clientId, message);
            }
        });
    }

    public void ackMessage(Jedis conn, String clientId, AckMessage message) {
        String clientPendingQueueName = RedisPushUtils.getClientPendingQueueName(clientId);
        String id;
        if (message.getId() != null) {
            id = message.getId().toString();
        } else if (message.getEvent() != null) {
            id = message.getEvent();
        } else {
            return;
        }
        conn.lrem(clientPendingQueueName, 0, id);
    }

    private PushMessage getMessage(Jedis conn, String clientId, String messageId) {
        String message = conn.get(RedisPushUtils.getMessageKeyName(clientId, messageId));
        if (message == null) {
            logger.debug("Message[" + messageId + "] expired for client[" + clientId + "]");
            return null;
        }

        logMessage(clientId, message);
        return RedisPushUtils.decodeMessage(message);
    }

    private void getAndSendPendingMessagesToClient(Jedis conn, String clientId) {
        PushClientListener client = clients.get(clientId);
        if (client == null) {
            //Client is no longer connected
            return;
        }
        if (!client.isConnected()) {
            removeClient(client.id());
            return;
        }
        List<PushMessage> messagesForClient = getPendingMessagesForClient(conn, client.id());
        logger.info("Sending " + messagesForClient.size() + "pending messages to " + client.id());
        for (PushMessage message : messagesForClient) {
            client.onMessage(message);
        }
    }


    private void getAndSendMessagesToClient(Jedis conn, String clientId) {
        PushClientListener client = clients.get(clientId);
        if (client == null) {
            //Client is no longer connected
            return;
        }
        if (!client.isConnected()) {
            removeClient(client.id());
            return;
        }
        logger.debug("Getting all messages for client[" + client.id() + "]");
        List<PushMessage> messagesForClient = getMessagesForClient(conn, client.id());
        for (PushMessage message : messagesForClient) {
            client.onMessage(message);
        }
    }

    private void waitForMessageAndSendToClient(Jedis conn) {
        String clientWithMessage = getNextClientWithMessage(conn, MAX_WAIT_FOR_NEW_MESSAGE_SEC);
        if (clientWithMessage != null) {
            if (RedisPushUtils.isNonPersistentMessage(clientWithMessage)) {
                sendNonPersistentMessage(clientWithMessage);
            } else {
                getAndSendMessagesToClient(conn, clientWithMessage);
            }
        }
    }

    private void sendNonPersistentMessage(String clientMessage) {
        String[] parts = clientMessage.split(":");
        String clientId = parts[0];
        Event event = new Event(parts[1], false);
        PushClientListener client = clients.get(clientId);
        if (client == null) {
            //Client is not connected.
            return;
        }
        if (!client.isConnected()) {
            //Client has disconnected but has not been removed.
            removeClient(client.id());
            return;
        }
        client.onMessage(new DefaultPushMessage(event, null, null));
    }

    private String getNextClientWithMessage(Jedis conn, int maxWait) {
        String globalQueueName = RedisPushUtils.getPoolSubscriberActionQueueName(subscriberName);
        List<String> clientWithMessage = conn.blpop(maxWait, globalQueueName);
        if (clientWithMessage == null) {
            return null;
        }
        return clientWithMessage.get(1);
    }

    private void runTasks(Jedis conn) {
        while (!redisTaskQueue.isEmpty()) {
            RedisTask task = redisTaskQueue.poll();
            task.runTask(conn);
        }
    }

    private void logJedisDisconnect(JedisConnectionException e) {
        String message = "Redis connection lost to pool " + poolId + "." +
                "Attempting to reconnect in " + redisReconnectDelayMs + "ms.";
        logger.warn(message, e);
    }

    private void logJedisDisconnectUnknownError(Exception e) {
        String message = "Unhandled Exception in pool: " + poolId + ". " +
                "Attempting to reconnect in " + redisReconnectDelayMs + "ms.";
        logger.error(message, e);
    }

    private void logMessage(String client, String message) {
        logger.info("Received for client[" + client + "] message was :" + message);
    }

    private class AddClientTask implements RedisTask {
        private final String clientId;

        AddClientTask(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void runTask(Jedis conn) {
            logger.info("Adding client[" + clientId + "] to pool.");
            String clientSet = RedisPushUtils.getActiveClientSetName(subscriberName);
            conn.sadd(clientSet, clientId);
            RedisPoolSubscriber.this.getAndSendPendingMessagesToClient(conn, clientId);
            RedisPoolSubscriber.this.getAndSendMessagesToClient(conn, clientId);
        }
    }

    private class RemoveClientTask implements RedisTask {
        private final String clientId;

        RemoveClientTask(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void runTask(Jedis conn) {
            logger.info("Removing client[" + clientId + "] from pool.");
            String clientSet = RedisPushUtils.getActiveClientSetName(subscriberName);
            conn.srem(clientSet, clientId);
        }
    }
}
