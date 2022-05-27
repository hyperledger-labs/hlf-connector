package hlf.java.rest.client.service;

import hlf.java.rest.client.model.ClientResponseModel;

public interface ConfigHandlerService {
  ClientResponseModel updateConfiguration(byte[] newConfigBytes);
}
