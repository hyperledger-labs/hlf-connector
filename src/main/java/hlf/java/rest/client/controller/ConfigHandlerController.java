package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.ConfigHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/configuration")
public class ConfigHandlerController {
  @Autowired private ConfigHandlerService configHandlerservice;
  @PostMapping("/update")
  public ResponseEntity<ClientResponseModel> handleFileUpload(@RequestBody byte[] fileInBytes)
      throws IOException {
    ClientResponseModel response = configHandlerservice.updateConfiguration(fileInBytes);
    return ResponseEntity.status(response.getCode()).body(response);
  }
}
