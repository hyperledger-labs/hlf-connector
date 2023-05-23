package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ClientResponseModel;
import org.springframework.http.ResponseEntity;

public interface EventFulfillment {

  /**
   * Replay the blockchain events.
   *
   * <p>Sends all the events from either the chaincode or the block itself since the specific block
   * number.
   *
   * @param startBlockNumber Long since the block number for replaying the events
   * @param endBlockNumber Long until the block number
   * @param networkName String channel name
   * @param eventType String event type parameter
   * @return responseEntity ResponseEntity Transaction Response
   */
  ResponseEntity<ClientResponseModel> replayEvents(
      Long startBlockNumber, Long endBlockNumber, String networkName, String eventType);
}
