package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.EventFulfillment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@ConditionalOnProperty(prefix = "fabric.events", name = "enable", havingValue = "true")
@RefreshScope
@Slf4j
public class EventController {

  @Autowired EventFulfillment eventFulfillment;

  /**
   * The REST Endpoint for replaying the events from a specific block number.
   *
   * @param startBlockNumber start block number
   * @param endBlockNumber end block number
   * @param networkName networkName/channel
   * @param eventType type of event
   * @return the ClientResponseModel status indicating that the request is accepted
   */
  @GetMapping(value = "/blocks/events")
  public ResponseEntity<ClientResponseModel> replayEvents(
      @RequestParam("block-number-start") Long startBlockNumber,
      @RequestParam("block-number-end") Long endBlockNumber,
      @RequestParam("channel") @Validated String networkName,
      @RequestParam("eventType") @Validated String eventType) {
    log.info(
        "Triggering blocks since: {}, to: {}, on: {}, type: {}",
        startBlockNumber,
        endBlockNumber,
        networkName,
        eventType);
    return eventFulfillment.replayEvents(startBlockNumber, endBlockNumber, networkName, eventType);
  }
}
