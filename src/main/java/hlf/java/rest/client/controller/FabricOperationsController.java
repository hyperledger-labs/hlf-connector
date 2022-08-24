package hlf.java.rest.client.controller;

import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.CommitChannelParamsDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.NetworkStatus;
import hlf.java.rest.client.util.SerializationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class FabricOperationsController {

  @Autowired private NetworkStatus networkStatus;

  @Autowired private SerializationUtil serializationUtil;

  /**
   * Obtain the channel configuration details.
   *
   * @param channelName - the name of the channel for which you will retrieve the channel
   *     configuration
   * @return ResponseEntity<ClientResponseModel> - contains the Base64 byte array of the channel
   *     configuration file
   */
  @GetMapping(value = "/channel/{channelName}/configuration")
  public ResponseEntity<ClientResponseModel> getChannelConfiguration(
      @PathVariable @Validated String channelName) {
    return networkStatus.getChannelFromNetwork(channelName);
  }

  /**
   * Obtain the Config Update file. The output of this function can be signed via {@linkplain
   * signChannelConfig}.
   *
   * @param channelName - the name of the channel for which you will generate the channel
   *     configuration update.
   * @param organizationDetails - contains the details for the organization you wish to be added to
   *     the configuration
   * @return ResponseEntity<ClientResponseModel> - contains the Base64 byte array of the channel
   *     update file
   */
  @PutMapping(value = "/channel/{channelName}/configuration/config_update")
  public ResponseEntity<ClientResponseModel> generateConfigUpdateFile(
      @PathVariable @Validated String channelName,
      @RequestBody @Validated NewOrgParamsDTO organizationDetails) {
    return networkStatus.generateConfigUpdate(channelName, organizationDetails);
  }

  /**
   * Sign a Channel Config file. The output of this function can be commited via {@linkplain
   * commitSignedChannelConfig}.
   *
   * @param channelName - the name of the channel for which you will retrieve the channel
   *     configuration
   * @param channelConfigUpdate - Byte array encoded as a Base64 String of the channel configuration
   *     edited with the desired updates
   * @return ResponseEntity<ClientResponseModel> - contains the Base64 byte array of the signed
   *     channel update file
   */
  @PutMapping(value = "/channel/{channelName}/configuration/signature")
  public ResponseEntity<ClientResponseModel> signChannelConfig(
      @PathVariable @Validated String channelName,
      @RequestBody @Validated String channelConfigUpdate) {
    return networkStatus.signChannelConfigTransaction(channelName, channelConfigUpdate);
  }

  /**
   * Commit the signed Channel Update file to the ledger.
   *
   * @param channelName - the name of the channel for which you will commit the channel update
   *     containing your required changes
   * @param commitChannelParamsDTO
   * @return ResponseEntity<ClientResponseModel> - contains the result of the operation
   */
  @PostMapping(value = "/channel/{channelName}/configuration/commitment")
  public ResponseEntity<ClientResponseModel> commitSignedChannelConfig(
      @PathVariable @Validated String channelName,
      @RequestBody @Validated CommitChannelParamsDTO commitChannelParamsDTO) {
    return networkStatus.commitChannelConfigTransaction(channelName, commitChannelParamsDTO);
  }

  /**
   * Add an organization to an existing file in one function.
   *
   * @param channelName - the name of the channel for which you will create, sign, and commit the
   *     channel update containing your required changes
   * @param organizationDetails - contains the details for the organization you wish to be added to
   *     the configuration
   * @return ResponseEntity<ClientResponseModel> - contains the result of the operation
   */
  @PostMapping(value = "/channel/{channelName}/new_org")
  public ResponseEntity<ClientResponseModel> addOrgToChannel(
      @PathVariable @Validated String channelName,
      @RequestBody @Validated NewOrgParamsDTO organizationDetails) {
    return networkStatus.addOrgToChannel(channelName, organizationDetails);
  }

  /**
   * Use to decode an base64 encoded json file, with options to also decode the interior elements
   * and/or print the output in a cleaner format
   *
   * @param decodeInterior - allows the interior elements of a file to be decoded. Do not use if
   *     file is not a standard object from the HLF library. Interior must be re-encoded becore use
   *     as an input for other functions.
   * @param prettyPrint - allows the output to be displayed in a more readible formatted. Do not use
   *     if file is not a standard object from the HLF library. Do not use if file is to be used as
   *     an input for other functions.
   * @param encodedJson - the base64 encoded file to decoded
   * @return ResponseEntity<ClientResponseModel> - contains the decoded file
   */
  @GetMapping(value = "/decode")
  public ResponseEntity<ClientResponseModel> getDeserializedJson(
      @RequestBody @Validated String encodedJson,
      @RequestParam(name = "decodeInterior", required = true) boolean decodeInterior,
      @RequestParam(name = "prettyPrint", required = true) boolean prettyPrint) {
    return serializationUtil.decodeContents(encodedJson, decodeInterior, prettyPrint);
  }
}
