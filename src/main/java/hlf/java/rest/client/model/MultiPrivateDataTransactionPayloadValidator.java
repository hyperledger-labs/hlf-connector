package hlf.java.rest.client.model;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

public class MultiPrivateDataTransactionPayloadValidator
    extends AbstractModelValidator<MultiDataTransactionPayload> {

  /**
   * -> Payload should at-least contain one or more Private Data or Public Data or both. -> If one
   * or more Private Data is present, it's corresponding key name and value name must be present.
   * CollectionName would remain as optional. -> Peer names are optional
   */
  @Override
  public void validate(MultiDataTransactionPayload multiDataTransactionPayload) {

    // Ensure that both Public & Private Data are not missing
    if (CollectionUtils.isEmpty(multiDataTransactionPayload.getPrivatePayload())
        && CollectionUtils.isEmpty(multiDataTransactionPayload.getPublicPayload())) {
      throw new IllegalStateException(
          "Payload should consist of at-least one Private Data Payload or Public Payload");
    }

    // If the model has Private data listed, ensure that it has mandatory fields populated
    // Have the nested if condition to avoid null pointer exceptions in case if the private
    // payload is absent and was never sent.
    if (!CollectionUtils.isEmpty(multiDataTransactionPayload.getPrivatePayload())) {
      boolean hasValidPrivateData =
          multiDataTransactionPayload.getPrivatePayload().stream()
              .allMatch(
                  privateTransactionPayload ->
                      StringUtils.isNotBlank(privateTransactionPayload.getKey())
                          && StringUtils.isNotBlank(privateTransactionPayload.getData()));

      if (!hasValidPrivateData) {
        throw new IllegalStateException(
            "One or more Private Data representation is invalid, Private details should contain both Key name & Data");
      }
    }
  }
}
