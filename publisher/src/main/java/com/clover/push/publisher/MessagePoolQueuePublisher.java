package com.clover.push.publisher;

import com.clover.push.message.DefaultPushMessage;
import com.clover.push.message.Event;
import com.clover.push.message.PushMessage;
import com.clover.push.redis.RedisPushUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static com.clover.push.redis.RedisPushUtils.encodeMessage;
import static com.clover.push.redis.RedisPushUtils.getActiveClientSetName;
import static com.clover.push.redis.RedisPushUtils.getClientQueueName;
import static com.clover.push.redis.RedisPushUtils.getMessageIdCounterQueueName;
import static com.clover.push.redis.RedisPushUtils.getMessageKeyName;
import static com.clover.push.redis.RedisPushUtils.getPoolSubscriberActionQueueName;

/**
 * User: josh
 * Date: 3/10/14
 */
class MessagePoolQueuePublisher implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(MessagePoolQueuePublisher.class);
  private static final int DEVICE_QUEUE_TIMEOUT_SEC = 60 * 60 * 24 * 5;
  private static final int MAX_DEVICE_QUEUE_LENGTH = 100;
  private static final int MAX_MESSAGES_TO_PROCESS_ON_SINGLE_RUN = 10000;


  private final BlockingQueue<PushMessageHolder> messageQueue;
  private final int poolId;
  private final JedisPool pool;
  public Set<String> enqueuedMessageTypes;
  Set<String> deviceQueuesToNotify;


  public MessagePoolQueuePublisher(BlockingQueue<PushMessageHolder> messageQueue, JedisPool pool, int poolId) {
    this.messageQueue = messageQueue;
    this.pool = pool;
    this.poolId = poolId;
  }

  @Override
  public void run() {
    if (messageQueue.isEmpty()) {
      return;
    }
    List<PushMessageHolder> processingMessages = Lists.newArrayList();

    messageQueue.drainTo(processingMessages, MAX_MESSAGES_TO_PROCESS_ON_SINGLE_RUN);

    Jedis conn = null;
    try {
      conn = pool.getResource();

      enqueuedMessageTypes = Sets.newHashSet();
      deviceQueuesToNotify = Sets.newHashSet();

      for (PushMessageHolder holder : processingMessages) {
        PushMessage message = holder.message;
        String clientKey = holder.clientKey;
        int timeoutSec = holder.timeoutSec;

        processMessage(conn, clientKey, message, timeoutSec);
      }

      notifyDevices(conn);

      enqueuedMessageTypes = null;
      deviceQueuesToNotify = null;
    } finally {
      pool.returnResource(conn);
    }
  }

  public void notifyDevices(Jedis conn) {
    Set<String> subscriberNames = conn.smembers(RedisPushUtils.SUBSCRIBER_QUEUE_LIST);
    for (String subscriberName : subscriberNames) {
      String subscriberActiveClientName = getActiveClientSetName(subscriberName);
      for (String clientKey : deviceQueuesToNotify) {
        if (conn.sismember(subscriberActiveClientName, clientKey)) {
          logger.debug("Pool[" + poolId + "]  Client[" + clientKey + "]  is online.");
          String globalQueueName = getPoolSubscriberActionQueueName(subscriberName);
          conn.lpush(globalQueueName, clientKey);
        }
      }
    }
  }

  public void processMessage(Jedis conn, String clientKey, PushMessage message, int timeoutSec) {
    String messageName = message.getEvent().getName();
    if (messageName != null && enqueuedMessageTypes.add(clientKey + ":" + messageName)) {
      if (Event.SEND_DEBUG.equals(message.getEvent()) || Event.REBOOT.equals(message.getEvent())) {
        enqueueNonPersistentEvent(conn, clientKey, messageName);
      } else if (message.getEvent().shouldGroup()) {
        enqueueSimpleEvent(conn, clientKey, messageName);
        deviceQueuesToNotify.add(clientKey);
      } else {
        enqueueMessage(conn, clientKey, message, timeoutSec);
        deviceQueuesToNotify.add(clientKey);
      }
    }
  }

  public static void enqueueSimpleEvent(Jedis conn, String clientOrGroup, String event) {
    String queueName = getClientQueueName(clientOrGroup);
    conn.lrem(queueName, 0, event);
    conn.lpush(queueName, event);
  }

  public static void enqueueNonPersistentEvent(Jedis conn, String clientOrGroup, String event) {
    Set<String> subscriberNames = conn.smembers(RedisPushUtils.SUBSCRIBER_QUEUE_LIST);
    for (String subscriberName : subscriberNames) {
      if (conn.sismember(getActiveClientSetName(subscriberName), clientOrGroup)) {
        String globalQueueName = getPoolSubscriberActionQueueName(subscriberName);
        conn.lpush(globalQueueName, clientOrGroup + ":" + event);
      }
    }
  }

  public void enqueueMessage(Jedis conn, String clientOrGroup,
                             PushMessage message, int messageTimeoutSec) {
    Long nextId = conn.incr(getMessageIdCounterQueueName(clientOrGroup));
    //TODO create message here?
    PushMessage newMessage = new DefaultPushMessage(nextId, message.getEvent(), message.getData(), message.getAppId());
    String strMessage = encodeMessage(newMessage);
    conn.setex(getMessageKeyName(clientOrGroup, nextId.toString()),
        messageTimeoutSec, strMessage);

    Long queueLength = conn.lpush(getClientQueueName(clientOrGroup), nextId.toString());
    if (queueLength > MAX_DEVICE_QUEUE_LENGTH) {
      //TODO better method of expiring.
      //for now just remove oldest entry
      conn.lpop(getClientQueueName(clientOrGroup));
    }
    conn.expire(getClientQueueName(clientOrGroup), DEVICE_QUEUE_TIMEOUT_SEC);
  }
}
