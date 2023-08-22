package hlf.java.rest.client.controller;

import hlf.java.rest.client.metrics.MetricsTrackedEndpoint;
import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.ChannelService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/channel")
public class ChannelOperationController {

  @Autowired private ChannelService channelService;

  @PostMapping("/create")
  @MetricsTrackedEndpoint(name = "Create Channel", method = "POST", uri = "/channel/create")
  public ResponseEntity<ClientResponseModel> createChannel(
      @RequestBody ChannelOperationRequest channelCreationRequest) {
    ClientResponseModel response = channelService.createChannel(channelCreationRequest);
    return ResponseEntity.status(response.getCode()).body(response);
  }

  @PostMapping("/join")
  @MetricsTrackedEndpoint(name = "Join Channel", method = "POST", uri = "/channel/join")
  public ResponseEntity<ClientResponseModel> joinChannel(
      @RequestBody ChannelOperationRequest channelJoinRequest) {
    ClientResponseModel response = channelService.joinChannel(channelJoinRequest);
    return ResponseEntity.status(response.getCode()).body(response);
  }

  @GetMapping("/members-mspid")
  public ResponseEntity<Set<String>> getChannelMembersMSPID(
      @RequestParam("channel_name") String channelName) {
    return new ResponseEntity<>(channelService.getChannelMembersMSPID(channelName), HttpStatus.OK);
  }
}
