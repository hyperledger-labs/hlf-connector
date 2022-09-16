package hlf.java.rest.client.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ErrorConstants;
import hlf.java.rest.client.exception.FabricTransactionException;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.CommitChannelParamsDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.AddOrgToChannelWriteSetBuilder;
import hlf.java.rest.client.service.ChannelConfigDeserialization;
import hlf.java.rest.client.service.NetworkStatus;
import java.io.IOException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.protos.common.Configtx.Config;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.common.Configtx.ConfigUpdate;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.UpdateChannelConfiguration;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkStatusImpl implements NetworkStatus {

  @Autowired private Gateway gateway;
  @Autowired private User user;
  @Autowired private AddOrgToChannelWriteSetBuilder addOrgToChannelWriteSetBuilder;
  @Autowired private ChannelConfigDeserialization channelConfigDeserialization;

  @Override
  /**
   * Obtains the channel configuration
   *
   * @param channelName - the name of the channel
   * @return ResponseEntity<ClientResponseModel> - Contains the channel configuration
   */
  public ResponseEntity<ClientResponseModel> getChannelFromNetwork(String channelName) {
    Network network = null;
    try {
      network = gateway.getNetwork(channelName);
      if (network != null) {
        Channel selectedChannel = network.getChannel();
        Config channelConfig = Config.parseFrom(selectedChannel.getChannelConfigurationBytes());
        MessageOrBuilder message = channelConfig;
        String channelConfigString = JsonFormat.printer().print(message);
        log.info(channelConfigString);
        String base64EncodedByteArraySerialized =
            Base64.getEncoder().encodeToString(channelConfigString.getBytes());
        return new ResponseEntity<>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, base64EncodedByteArraySerialized),
            HttpStatus.OK);
      }
    } catch (InvalidArgumentException e) {
      log.warn(
          "Error retrieving channel config: One or more arguments included in the config update are invalid");
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
          "One or more arguments included in the config update are invalid",
          e);
    } catch (TransactionException e) {
      log.warn("Error retrieving channel config: " + e.getMessage());
      throw new FabricTransactionException(
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
          ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR.name(),
          e);
    } catch (IOException e) {
      log.warn("Error while establishing connection to the gateway");
      throw new ServiceException(
          ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR,
          "Error while establishing connection to the gateway",
          e);
    }
    log.warn("Error getting channel config: Network cannot be NULL: " + "Network = " + network);
    return new ResponseEntity<>(
        new ClientResponseModel(ErrorCode.NOT_FOUND.getValue(), ErrorCode.NOT_FOUND.name()),
        HttpStatus.OK);
  }

  @Override
  public ResponseEntity<ClientResponseModel> generateConfigUpdate(
      String channelName, NewOrgParamsDTO organizationDetails) {
    Network network = gateway.getNetwork(channelName);

    if (network != null) {
      try {
        Channel selectedChannel = network.getChannel();
        ConfigUpdate selectedChannelConfigUpdate =
            ConfigUpdate.parseFrom(selectedChannel.getChannelConfigurationBytes());
        // ReadSet must have version, all other fields ignored; WriteSet must have changes
        ConfigGroup readset = selectedChannelConfigUpdate.getReadSet();
        // ConfigGroups consist of: groups, modPolicy, policies, values, and version.
        ConfigUpdate configUpdate =
            ConfigUpdate.newBuilder()
                .setChannelId(channelName)
                .setReadSet(readset)
                .setWriteSet(
                    addOrgToChannelWriteSetBuilder.buildWriteset(readset, organizationDetails))
                .build();
        MessageOrBuilder message = configUpdate;
        String channelConfigString = JsonFormat.printer().print(message);
        log.info(channelConfigString);
        String base64EncodedByteArrayDeserialized =
            Base64.getEncoder().encodeToString(channelConfigString.getBytes());
        return new ResponseEntity<>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, base64EncodedByteArrayDeserialized),
            HttpStatus.OK);
      } catch (InvalidArgumentException e) {
        log.warn(
            "Error while committing channel config: One or more arguments included in the config update are invalid");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "One or more arguments included in the config update are invalid",
            e);
      } catch (TransactionException e) {
        log.warn("Error while committing channel config: " + e.getMessage());
        throw new FabricTransactionException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR.name(),
            e);
      } catch (IOException e) {
        log.warn("Error while establishing connection to the gateway");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR,
            "Error while establishing connection to the gateway",
            e);
      }
    } else {
      log.warn(
          "Error generating the Config Update: Network and User cannot be NULL: "
              + "Network = "
              + network);
      return new ResponseEntity<>(
          new ClientResponseModel(
              ErrorCode.NOT_FOUND.getValue(),
              "Network and User cannot be NULL: " + "Network = " + network),
          HttpStatus.OK);
    }
  }

  /**
   * Signs the channel configuration json using the credentials owned by this application's
   * organization
   *
   * @param channelName - the name of the channel
   * @param configUpdateEncoded - the channel config update byte array in a base64 encoded String to
   *     be signed
   * @return ResponseEntity<ClientResponseModel> - Contains the signed channel configuration byte
   *     array in a base64 encoded String
   */
  public ResponseEntity<ClientResponseModel> signChannelConfigTransaction(
      String channelName, String configUpdateEncoded) {
    Network network = gateway.getNetwork(channelName);
    if (network != null && user != null) {
      try {
        Channel selectedChannel = network.getChannel();
        byte[] configUpdateDecodedByteArray = Base64.getDecoder().decode(configUpdateEncoded);
        String configUpdateDecoded = new String(configUpdateDecodedByteArray);
        Builder builder = ConfigUpdate.newBuilder();
        JsonFormat.parser().merge(configUpdateDecoded, builder);
        ConfigUpdate configUpdate = (ConfigUpdate) builder.build();
        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration();
        updateChannelConfiguration.setUpdateChannelConfiguration(
            configUpdate.toByteString().toByteArray());
        // ConfigSignature as a byte[]. Already base64 encoded so no additonal encoding needed.
        // User/client owner will change org ownership based on wallet ids
        byte[] signedChannelUpdate =
            selectedChannel.getUpdateChannelConfigurationSignature(
                updateChannelConfiguration, user);
        return new ResponseEntity<>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, signedChannelUpdate), HttpStatus.OK);

      } catch (InvalidArgumentException e) {
        log.warn(
            "Error while signing channel config: One or more arguments included in the config update are invalid.");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "One or more arguments included in the config update are invalid",
            e);
      } catch (InvalidProtocolBufferException e) {
        log.warn("Failed to parse incoming ConfigUpdate String in ConfigUpdate protobuf.");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "Failed to parse incoming ConfigUpdate String in ConfigUpdate protobuf",
            e);
      }
    } else {
      log.warn(
          "Error while signing channel config: Network and User cannot be NULL: "
              + "Network = "
              + network
              + "and User = "
              + user);
      return new ResponseEntity<>(
          new ClientResponseModel(
              ErrorCode.NOT_FOUND.getValue(),
              "Network and User cannot be NULL: " + "Network = " + network + "and User = " + user),
          HttpStatus.OK);
    }
  }

  @Override
  /**
   * Commits the channel configuration using the credentials owned by this application's
   * organization to the orderer for persistance to the ledger as part of a config block
   *
   * @param channelName - the name of the channel
   * @param configJsonMap - contains the channel config json and the signers: - configUpdate: the
   *     unsigned configuration updated - signer(s): base64 encoded byte[] containing the signed
   *     channel config, one for each required organization. If there is more than one signer then
   *     the "Key" = signer_<n> where <n> is a number > 0 and incremented by 1 for each additional
   *     signer. (ex: signer_1, signer_2, etc.)
   * @return ResponseEntity<ClientResponseModel> - returns whether the update was successfully
   *     submitted to the orderer or failed to be accepted
   */
  public ResponseEntity<ClientResponseModel> commitChannelConfigTransaction(
      String channelName, CommitChannelParamsDTO commitChannelParamsDTO) {
    byte[][] signers = new byte[commitChannelParamsDTO.getSignatures().size()][];
    int signerArrayIndex = 0;
    for (byte[] signature : commitChannelParamsDTO.getSignatures()) {
      signers[signerArrayIndex++] = signature;
    }
    Network network = gateway.getNetwork(channelName);
    if (network != null && user != null) {
      try {
        byte[] configUpdateDecodedByteArray =
            Base64.getDecoder().decode(commitChannelParamsDTO.getConfigUpdateBase64Encoded());
        String configUpdateDecoded = new String(configUpdateDecodedByteArray);
        Builder builder = ConfigUpdate.newBuilder();
        JsonFormat.parser().merge(configUpdateDecoded, builder);
        ConfigUpdate configUpdate = (ConfigUpdate) builder.build();

        Channel selectedChannel = network.getChannel();
        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration();
        updateChannelConfiguration.setUpdateChannelConfiguration(
            configUpdate.toByteString().toByteArray());
        selectedChannel.updateChannelConfiguration(updateChannelConfiguration, signers);
        return new ResponseEntity<>(
            new ClientResponseModel(ErrorConstants.NO_ERROR, ErrorCode.SUCCESS.getReason()),
            HttpStatus.OK);
      } catch (InvalidArgumentException e) {
        log.warn(
            "Error while committing channel config: One or more arguments included in the config update are invalid");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "One or more arguments included in the config update are invalid",
            e);
      } catch (TransactionException e) {
        log.warn("Error while committing channel config: " + e.getMessage());
        throw new FabricTransactionException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR.name(),
            e);
      } catch (InvalidProtocolBufferException e) {
        log.warn("Failed to parse incoming ConfigUpdate String in ConfigUpdate protobuf.");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "Failed to parse incoming ConfigUpdate String in ConfigUpdate protobuf",
            e);
      }
    } else {
      log.warn(
          "Error while committing channel config: Network and User cannot be NULL: "
              + "Network = "
              + network
              + "and User = "
              + user);
      return new ResponseEntity<>(
          new ClientResponseModel(
              ErrorCode.NOT_FOUND.getValue(),
              "Network and User cannot be NULL: " + "Network = " + network + "and User = " + user),
          HttpStatus.OK);
    }
  }

  @Override
  public ResponseEntity<ClientResponseModel> addOrgToChannel(
      String channelName, NewOrgParamsDTO organizationDetails) {
    Network network = gateway.getNetwork(channelName);
    if (network != null && user != null) {
      try {
        Channel selectedChannel = network.getChannel();
        ConfigUpdate selectedChannelConfigUpdate =
            ConfigUpdate.parseFrom(selectedChannel.getChannelConfigurationBytes());
        // ReadSet must have version, all other fields ignored; WriteSet must have changes
        ConfigGroup readset = selectedChannelConfigUpdate.getReadSet();
        // ConfigGroups consist of: groups, modPolicy, policies, values, and version.
        ConfigUpdate configUpdate =
            ConfigUpdate.newBuilder()
                .setChannelId(channelName)
                .setReadSet(readset)
                .setWriteSet(
                    addOrgToChannelWriteSetBuilder.buildWriteset(readset, organizationDetails))
                .build();
        MessageOrBuilder message = configUpdate;
        String channelConfigString = JsonFormat.printer().print(message);
        log.info(channelConfigDeserialization.deserializeValueFields(channelConfigString));
        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration();
        updateChannelConfiguration.setUpdateChannelConfiguration(
            configUpdate.toByteString().toByteArray());
        selectedChannel.updateChannelConfiguration(
            updateChannelConfiguration,
            selectedChannel.getUpdateChannelConfigurationSignature(
                updateChannelConfiguration, user));
      } catch (InvalidArgumentException e) {
        log.warn(
            "Error while committing channel config: One or more arguments included in the config update are invalid");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            "One or more arguments included in the config update are invalid",
            e);
      } catch (TransactionException e) {
        log.warn("Error while committing channel config: " + e.getMessage());
        throw new FabricTransactionException(
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR,
            ErrorCode.HYPERLEDGER_FABRIC_TRANSACTION_ERROR.name(),
            e);
      } catch (IOException e) {
        log.warn("Error while establishing connection to the gateway");
        throw new ServiceException(
            ErrorCode.HYPERLEDGER_FABRIC_CONNECTION_ERROR,
            "Error while establishing connection to the gateway",
            e);
      }
      return new ResponseEntity<>(
          new ClientResponseModel(ErrorConstants.NO_ERROR, ErrorCode.SUCCESS.getReason()),
          HttpStatus.OK);
    } else {
      log.warn("Network and User cannot be NULL: " + "Network = " + network + "and User = " + user);
      return new ResponseEntity<>(
          new ClientResponseModel(
              ErrorCode.NOT_FOUND.getValue(),
              "Network and User cannot be NULL: " + "Network = " + network + "and User = " + user),
          HttpStatus.OK);
    }
  }
}
