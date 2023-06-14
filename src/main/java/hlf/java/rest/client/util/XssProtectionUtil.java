package hlf.java.rest.client.util;

import static hlf.java.rest.client.exception.ErrorCode.XSS_VALIDATION_FAILED;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

import hlf.java.rest.client.exception.InputValidationException;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@UtilityClass
public class XssProtectionUtil {

  public String validateAndGetXssSafeString(String inputArg) {
    if (!escapeHtml4(inputArg).equals(inputArg)) {
      throw new InputValidationException(
          XSS_VALIDATION_FAILED,
          "Xss validation failed, parameter: " + inputArg + " may contain Xss strings");
    }
    return inputArg;
  }

  public static MultipartFile validateAndGetXssSafeFile(MultipartFile file) throws IOException {
    String fileName = file.getOriginalFilename();
    String content = new String(file.getBytes());
    if (!escapeHtml4(content).equals(content) && !escapeHtml4(fileName).equals(fileName)) {
      throw new InputValidationException(
          XSS_VALIDATION_FAILED, "Xss validation failed, file may contain Xss strings");
    }
    return file;
  }
}
