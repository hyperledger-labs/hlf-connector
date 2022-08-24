package hlf.java.rest.client.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.Base64;
import org.hyperledger.fabric.protos.common.Configuration;
import org.junit.jupiter.api.Test;

public class TestingUtlitlies {

  @Test
  public void encodeStringToABase64EncodedString() {
    String unencodedString = "*Hello There*";
    String encodedString = new String(Base64.getEncoder().encode(unencodedString.getBytes()));
    System.out.println(encodedString);
  }

  @Test
  public void decodeABase64EncodedString() throws InvalidProtocolBufferException {
    String base64EncodedByteArrayString = "ChBTYW1wbGVDb25zb3J0aXVt";
    byte[] byteArray = Base64.getDecoder().decode(base64EncodedByteArrayString);
    Configuration.Consortium consortium =
        org.hyperledger.fabric.protos.common.Configuration.Consortium.parseFrom(byteArray);
    String decodedString = JsonFormat.printer().print(consortium);
    System.out.println(decodedString);
  }
}
