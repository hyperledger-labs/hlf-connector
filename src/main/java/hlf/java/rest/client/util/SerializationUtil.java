package hlf.java.rest.client.util;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.service.ChannelConfigDeserialization;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SerializationUtil {

  @Autowired private ChannelConfigDeserialization channelConfigDeserialization;

  public ResponseEntity<ClientResponseModel> decodeContents(
      String encodedJson, boolean decodeInterior, boolean prettyPrint) {
    byte[] decodedJsonByteArray = Base64.getDecoder().decode(encodedJson);
    String decodedJson = new String(decodedJsonByteArray);
    try {
      if (decodeInterior) {
        decodedJson = channelConfigDeserialization.deserializeValueFields(decodedJson);
      }
      if (prettyPrint) {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(decodedJson);
        return new ResponseEntity<>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, json), HttpStatus.OK);
      }
      return new ResponseEntity<>(
          new ClientResponseModel(ErrorConstants.NO_ERROR, decodedJson), HttpStatus.OK);
    } catch (Exception e) {
      log.warn("Json Deserialization Failed");
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Json Deserialization Failed", e);
    }
  }
}
