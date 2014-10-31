package com.clover.push.redis;

import com.clover.push.message.AckMessage;
import com.clover.push.message.DefaultPushMessage;
import com.clover.push.message.PushMessage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: josh
 * Date: 1/13/14
 */

public class RedisPushUtils {
  private static final Logger logger = LoggerFactory.getLogger(RedisPushUtils.class);

  public static final String SUBSCRIBER_QUEUE_LIST = "p:sq";

  private static final String QUEUE_CLIENT_SET_PREFIX = "p::cs::";
  private static final String GLOBAL_SUBSCRIBER_QUEUE_PREFIX = "p::sq::";
  private static final String MESSAGE_ID_COUNTER_QUEUE_PREFIX = "p::mc::";
  private static final String CLIENT_QUEUE_PREFIX = "p::c::";
  private static final String CLIENT_PENDING_QUEUE_PREFIX = "p::pc::";
  private static final String MESSAGE_KEY = "::m::";


  //TODO replace with custom serialization
  public static final ObjectMapper mapper;
  static {
    mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }


  public static String getMessageKeyName(String clientId, String messageId) {
    return CLIENT_QUEUE_PREFIX + clientId + MESSAGE_KEY + messageId;
  }

  public static String getPoolSubscriberActionQueueName(String queueId) {
    return GLOBAL_SUBSCRIBER_QUEUE_PREFIX + queueId;
  }

  public static String getMessageIdCounterQueueName(String clientId) {
    return MESSAGE_ID_COUNTER_QUEUE_PREFIX + clientId;
  }

  public static String getClientQueueName(String clientId) {
    return CLIENT_QUEUE_PREFIX + clientId;
  }

  public static String getClientPendingQueueName(String clientId) {
    return CLIENT_PENDING_QUEUE_PREFIX + clientId;
  }

  public static String getSubscriberName(String hash, String queueId ) {
    return hash + "::p::" + queueId;
  }

  public static String getActiveClientSetName(String fullSubscriberName) {
    return QUEUE_CLIENT_SET_PREFIX + fullSubscriberName;
  }

  public static boolean isInteger(String str) {
    return str.matches("\\d*");
  }

  public static boolean isNonPersistentMessage(String clientWithMessage) {
    return clientWithMessage.contains(":");
  }



  public static String encodeMessage(PushMessage message) {
    try {
      return mapper.writeValueAsString(message);
    } catch (JsonProcessingException e) {
      logger.warn("Unable to encode message", e);
      return null;
    }
  }

  public static PushMessage decodeMessage(String message) {
    try {
      return mapper.readValue(message,DefaultPushMessage.class);
    } catch (IOException e) {
      logger.warn("Unable to decode message.", e);
      return null;
    }
  }

  public static AckMessage decodeAckMessage(String message) {
    try {
      return mapper.readValue(message, AckMessage.class);
    } catch (IOException e) {
      logger.warn("Unable to decode message.", e);
      return null;
    }
  }

  public static int selectPool(String clientId, int numOfPools) {
    return Math.abs(clientId.hashCode() % numOfPools);
  }
}
