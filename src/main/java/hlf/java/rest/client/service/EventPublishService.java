package hlf.java.rest.client.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * The EventPublishService is a service class, which include the kafka template. It sends the
 * Message to the Event Kafka message topic
 */
@ConditionalOnProperty("kafka.event-listener.brokerHost")
public interface EventPublishService {

  /**
   * @param payload String message payload
   * @param fabricTxId String Fabric transaction ID
   * @param eventName String chaincode event-name
   * @param channelName String Name of the channel where the event was generated.
   * @return status boolean status of msg sent
   */
  boolean sendMessage(
      final String payload, String fabricTxId, String eventName, String channelName);

  /**
   * @param payload String message payload
   * @param fabricTxId String Fabric transaction ID
   * @param eventName String chaincode event-name
   * @param channelName String Name of the channel where the event was generated.
   * @param messageKey associated key for the payload.
   * @return status boolean status of msg sent
   */
  boolean publishChaincodeEvents(
      final String payload,
      String chaincodeName,
      String fabricTxId,
      String eventName,
      String channelName,
      String messageKey);

  /**
   * @param payload String message payload
   * @param fabricTxId String Fabric transaction ID
   * @param chaincodeName String chaincode name
   * @param channelName String Name of the channel where the event was generated.
   * @param functionName String Name of the function name.
   * @param isPrivateDataPresent boolean flag to check if privateData present in payload
   * @return status boolean status of msg sent
   */
  boolean publishBlockEvents(
      final String payload,
      String fabricTxId,
      String channelName,
      String chaincodeName,
      String functionName,
      Boolean isPrivateDataPresent);
}
