package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.ChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/channel")
public class ChannelOperationController {

  @Autowired private ChannelService channelService;

  @PostMapping("/create")
  public ResponseEntity<ClientResponseModel> createChannel(
      @RequestBody ChannelOperationRequest channelCreationRequest) {
    ClientResponseModel response = channelService.createChannel(channelCreationRequest);
    return ResponseEntity.status(response.getCode()).body(response);
  }

  @PostMapping("/join")
  public ResponseEntity<ClientResponseModel> joinChannel(
      @RequestBody ChannelOperationRequest channelJoinRequest) {
    ClientResponseModel response = channelService.joinChannel(channelJoinRequest);
    return ResponseEntity.status(response.getCode()).body(response);
  }
}
