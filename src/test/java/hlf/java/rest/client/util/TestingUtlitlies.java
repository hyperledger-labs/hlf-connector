package hlf.java.rest.client.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class TestingUtlitlies {

  @Test
  public void encodeStringToABase64EncodedString() {
    String unencodedString = "*Hello There*";
    String encodedString = new String(Base64.getEncoder().encode(unencodedString.getBytes()));
    System.out.println(encodedString);
  }

  @Test
  public void decodeABase64EncodedString() {
    String base64EncodedByteArrayString = "KkhlbGxvIFRoZXJlKg==";
    String decodedString = new String(Base64.getDecoder().decode(base64EncodedByteArrayString),
        StandardCharsets.ISO_8859_1);
    System.out.println(decodedString);
  }

}
