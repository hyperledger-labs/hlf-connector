package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.config.ServerProperties;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.ConfigHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class ConfigHandlerServiceImpl implements ConfigHandlerService {

  @Autowired private ServerProperties serverProperties;

  @Override
  public ClientResponseModel updateConfiguration(byte[] newConfigBytes) {
    try {
      String fileLocation = serverProperties.getConfigLocation();
      File path = new File(fileLocation);
      FileUtils.writeByteArrayToFile(path, newConfigBytes);
      return new ClientResponseModel(HttpStatus.SC_OK, "File is uploaded successfully!");
    } catch (Exception e) {
      log.info("File upload failed");
      return new ClientResponseModel(HttpStatus.SC_INTERNAL_SERVER_ERROR, "File upload failed!");
    }
  }
}
