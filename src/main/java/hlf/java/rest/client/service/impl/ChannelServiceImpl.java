package hlf.java.rest.client.service.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import hlf.java.rest.client.exception.ChannelOperationException;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ChannelOperationType;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.MSPDTO;
import hlf.java.rest.client.service.ChannelService;
import hlf.java.rest.client.service.HFClientWrapper;
import hlf.java.rest.client.util.FabricChannelUtil;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.common.Configtx;
import org.hyperledger.fabric.protos.common.Configuration;
import org.hyperledger.fabric.protos.common.Policies;
import org.hyperledger.fabric.protos.msp.MspConfigPackage;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class ChannelServiceImpl implements ChannelService {

  @Autowired private HFClientWrapper hfClientWrapper;
  @Autowired private User user;
  @Autowired private Gateway gateway;

  private static final String DEFAULT_MOD_POLICY = "Admins";
  private static final String EMPTY_MOD_POLICY = "";
  private static final String GROUP_TAG_APPLICATION = "Application";
  private static final String VALUE_TAG_CONSORTIUM = "Consortium";
  private static final String VALUE_TAG_CAPABILITIES = "Capabilities";
  private static final String FABRIC_2_0 = "V2_0";
  private static final int INIT_VERSION = 1;
  private static final int EMPTY_VERSION = 0;
  private static final int HEADER_TYPE = 2;
  private static final String PEER_ORGANIZATION_MSPID_PROPERTY =
      "org.hyperledger.fabric.sdk.peer.organization_mspid";

  /** {@inheritDoc} */
  @Override
  public ClientResponseModel createChannel(ChannelOperationRequest channelOperationRequest) {
    ErrorCode validationResult =
        validateRequest(channelOperationRequest, ChannelOperationType.CREATE);
    if (validationResult != null) {
      throw new ChannelOperationException(validationResult);
    }

    try {
      List<Peer> peers = channelOperationRequest.getFabricPeers(hfClientWrapper.getHfClient());
      String channelName = channelOperationRequest.getChannelName();

      // check if the channel exists in any peer of the user org who initiated the creation,
      // operation fails
      for (Peer peer : peers) {
        if (peer.getProperties()
            .getProperty(PEER_ORGANIZATION_MSPID_PROPERTY)
            .equals(user.getMspId())) {
          Set<String> channels = hfClientWrapper.getHfClient().queryChannels(peer);
          if (channels.contains(channelName)) {
            log.error(
                "{} {} {}",
                ErrorCode.CHANNEL_CREATION_CHANNEL_EXISTS.getReason(),
                channelName,
                peer);
            throw new ChannelOperationException(ErrorCode.CHANNEL_CREATION_CHANNEL_EXISTS);
          }
        }
      }

      List<Orderer> orderers =
          channelOperationRequest.getFabricOrderers(hfClientWrapper.getHfClient());
      // TODO: Loop through all orderers in case of failing to talk to one of them
      Orderer orderer = orderers.get(0);

      // generate new channel config
      ChannelConfiguration channelConfiguration = newChannelConfig(channelOperationRequest);
      byte[] orgSignature =
          hfClientWrapper
              .getHfClient()
              .getChannelConfigurationSignature(channelConfiguration, user);
      hfClientWrapper
          .getHfClient()
          .newChannel(channelName, orderer, channelConfiguration, orgSignature);

      return new ClientResponseModel(HttpStatus.SC_OK, channelName + " is created");
    } catch (Exception e) {
      log.error("Error in creating new channel: ", e);
      throw new ChannelOperationException(
          ErrorCode.CHANNEL_CREATION_FAILED, "channel creation failed", e.getCause());
    }
  }

  /** {@inheritDoc} */
  @Override
  public ClientResponseModel joinChannel(ChannelOperationRequest channelOperationRequest) {
    ErrorCode validationResult =
        validateRequest(channelOperationRequest, ChannelOperationType.JOIN_PEER);
    if (validationResult != null) {
      throw new ChannelOperationException(validationResult);
    }

    Channel channel;
    try {
      String channelName = channelOperationRequest.getChannelName();

      List<Peer> peers = channelOperationRequest.getFabricPeers(hfClientWrapper.getHfClient());
      List<Orderer> orderers =
          channelOperationRequest.getFabricOrderers(hfClientWrapper.getHfClient());
      Orderer orderer = orderers.get(0);

      channel = hfClientWrapper.getHfClient().getChannel(channelName);
      if (channel == null) {
        channel = hfClientWrapper.getHfClient().newChannel(channelName);
      }

      for (Peer peer : peers) {
        if (peer.getProperties()
            .getProperty(PEER_ORGANIZATION_MSPID_PROPERTY)
            .equals(user.getMspId())) {
          Set<String> channels = hfClientWrapper.getHfClient().queryChannels(peer);
          if (!channels.contains(channelName)) {
            channel.addOrderer(orderer);
            channel.joinPeer(peer);
          }
        } else {
          throw new ServiceException(
              ErrorCode.NOT_SUPPORTED,
              "The peer join operation is unauthorized, peer belongs to unexcepted MSP");
        }
      }

      return new ClientResponseModel(HttpStatus.SC_OK, channelName + " joined");
    } catch (Exception e) {
      log.error("Error in joining channel ", e);
      throw new ChannelOperationException(
          ErrorCode.CHANNEL_JOIN_FAILED, "channel join failed", e.getCause());
    }
  }

  @Override
  public Set<String> getChannelMembersMSPID(String channelName) {

    if (StringUtils.isBlank(channelName)) {
      throw new ChannelOperationException(ErrorCode.CHANNEL_NAME_MISSING);
    }
    Network network = gateway.getNetwork(channelName);
    Channel channel = network.getChannel();
    try {
      channel.getChannelConfigurationBytes();
      return new HashSet<>(channel.getPeersOrganizationMSPIDs());
    } catch (Exception e) {
      throw new ChannelOperationException(ErrorCode.CHANNEL_NOT_FOUND);
    }
  }

  /**
   * create new channel configuration
   *
   * @param channelOperationRequest that contains the requested information for creating the channel
   * @return channel configuration
   */
  private ChannelConfiguration newChannelConfig(ChannelOperationRequest channelOperationRequest) {
    ChannelConfiguration newChannelConfig = new ChannelConfiguration();
    try {
      Common.Envelope newChannelEnvelop = getChannelCreationTransaction(channelOperationRequest);
      newChannelConfig.setChannelConfiguration(newChannelEnvelop.toByteArray());
    } catch (InvalidArgumentException e) {
      throw new ChannelOperationException(
          ErrorCode.CHANNEL_PAYLOAD_ERROR, "invalid argument", e.getCause());
    }
    return newChannelConfig;
  }

  /**
   * generate transaction envelope using default configuration
   *
   * @param channelName channel name to be created
   * @param configUpdate channel configuration
   * @return configuration envelope
   */
  public Common.Envelope getChannelCreationTransaction(
      String channelName, Configtx.ConfigUpdate configUpdate) {
    Configtx.ConfigUpdateEnvelope configUpdateEnvelope =
        Configtx.ConfigUpdateEnvelope.newBuilder()
            .setConfigUpdate(configUpdate.toByteString())
            .build();
    Common.ChannelHeader.Builder channelHeaderBuilder =
        Common.ChannelHeader.newBuilder().setTlsCertHash(ByteString.EMPTY);
    Timestamp.Builder timestampBuilder = Timestamp.newBuilder();
    timestampBuilder.setSeconds(System.currentTimeMillis() / 1000).setNanos(0);
    channelHeaderBuilder
        .setType(HEADER_TYPE)
        .setVersion(EMPTY_VERSION)
        .setChannelId(channelName)
        .setTimestamp(timestampBuilder)
        .setEpoch(0);
    Common.Header header =
        Common.Header.newBuilder()
            .setChannelHeader(channelHeaderBuilder.build().toByteString())
            .build();
    Common.Payload.Builder payloadBuilder =
        Common.Payload.newBuilder().setData(configUpdateEnvelope.toByteString()).setHeader(header);
    return Common.Envelope.newBuilder()
        .setPayload(payloadBuilder.build().toByteString())
        .setSignature(ByteString.EMPTY)
        .build();
  }

  /**
   * generate transaction envelope using default configuration
   *
   * @param channelOperationRequest request of channel creation
   * @return configuration envelope
   */
  private Common.Envelope getChannelCreationTransaction(
      ChannelOperationRequest channelOperationRequest) {
    Configtx.ConfigUpdate configUpdate = newConfigUpdate(channelOperationRequest);
    return getChannelCreationTransaction(channelOperationRequest.getChannelName(), configUpdate);
  }

  /**
   * config update for channel creation: empty readset with msp listed writeset with default setting
   *
   * @param channelOperationRequest incoming request from the caller of the API
   * @return config update
   */
  private Configtx.ConfigUpdate newConfigUpdate(ChannelOperationRequest channelOperationRequest) {
    Map<String, MSPDTO> mspMap = new HashMap<>();
    channelOperationRequest
        .getPeers()
        .forEach(p -> mspMap.putIfAbsent(p.getMspid(), p.getMspDTO()));
    return Configtx.ConfigUpdate.newBuilder()
        .setChannelId(channelOperationRequest.getChannelName())
        .setReadSet(newChannelGroup(channelOperationRequest.getConsortiumName(), mspMap, false))
        .setWriteSet(newChannelGroup(channelOperationRequest.getConsortiumName(), mspMap, true))
        .build();
  }

  /**
   * generate default read/write set
   *
   * @param consortiumName consortium where the channel is created
   * @param mspMap MSPID to MSPDTO mapping for all the to-be added organizations
   * @param isWriteSet true if it's for a writeset, false if it's for a readset
   * @return new channel group information with the constructed payload for either read/write set
   */
  private Configtx.ConfigGroup newChannelGroup(
      String consortiumName, Map<String, MSPDTO> mspMap, boolean isWriteSet) {
    Configtx.ConfigGroup.Builder channelGroupBuilder = Configtx.ConfigGroup.newBuilder();
    channelGroupBuilder.putGroups(GROUP_TAG_APPLICATION, newApplicationGroup(mspMap, isWriteSet));
    if (isWriteSet) {
      channelGroupBuilder.putValues(VALUE_TAG_CONSORTIUM, getConsortium(consortiumName));
    } else {
      channelGroupBuilder.putValues(
          VALUE_TAG_CONSORTIUM, Configtx.ConfigValue.newBuilder().build());
    }
    channelGroupBuilder.setVersion(EMPTY_VERSION).setModPolicy(EMPTY_MOD_POLICY);
    // add capabilities for V2_0 lifecycle chaincode support
    channelGroupBuilder.putValues(VALUE_TAG_CAPABILITIES, getCapabilities(FABRIC_2_0));
    return channelGroupBuilder.build();
  }

  /**
   * generate the application group
   *
   * @param mspMap MSPID to MSPDTO mapping of all the organizations to be added
   * @param isWriteSet true if it's for a writeset, false if it's for a readset
   * @return application config group
   */
  private Configtx.ConfigGroup newApplicationGroup(Map<String, MSPDTO> mspMap, boolean isWriteSet) {
    Configtx.ConfigGroup.Builder appGroupBuilder = Configtx.ConfigGroup.newBuilder();
    appGroupBuilder.setModPolicy(DEFAULT_MOD_POLICY).setVersion(EMPTY_VERSION);
    if (isWriteSet) {
      addDefaultImplicitMetaPolicy(appGroupBuilder);
      appGroupBuilder.setVersion(INIT_VERSION);
      appGroupBuilder.putValues(VALUE_TAG_CAPABILITIES, getCapabilities(FABRIC_2_0));
    }
    for (Map.Entry<String, MSPDTO> entry : mspMap.entrySet()) {
      if (entry.getValue() != null) {
        appGroupBuilder.putGroups(
            entry.getKey(), getMSPConfigGroup(entry.getKey(), entry.getValue()));
      } else {
        appGroupBuilder.putGroups(entry.getKey(), emptyMSPConfigGroup());
      }
    }
    return appGroupBuilder.build();
  }

  /**
   * @param capabilities capabilities need to be added to config
   * @return channel capabilities
   */
  private Configtx.ConfigValue getCapabilities(String... capabilities) {
    Configtx.ConfigValue.Builder valueBuilder = Configtx.ConfigValue.newBuilder();
    valueBuilder.setModPolicy(DEFAULT_MOD_POLICY);
    Configuration.Capabilities.Builder capabilitiesBuilder =
        Configuration.Capabilities.newBuilder();
    for (String capability : capabilities) {
      capabilitiesBuilder.putCapabilities(
          capability, Configuration.Capability.newBuilder().build());
    }
    valueBuilder.setValue(capabilitiesBuilder.build().toByteString());
    return valueBuilder.build();
  }

  /**
   * generate empty msp config group
   *
   * @return msp config group
   */
  private Configtx.ConfigGroup emptyMSPConfigGroup() {
    return Configtx.ConfigGroup.newBuilder()
        .setVersion(EMPTY_VERSION)
        .putAllGroups(new HashMap<>())
        .setModPolicy(EMPTY_MOD_POLICY)
        .putAllPolicies(new HashMap<>())
        .putAllValues(new HashMap<>())
        .build();
  }

  /**
   * generate msp config group based on the mspdto passed
   *
   * @param mspId organization's MSP ID
   * @param msp organization's MSP information
   * @return policies set as config group for that particular MSP
   */
  private Configtx.ConfigGroup getMSPConfigGroup(String mspId, MSPDTO msp) {
    Map<String, Configtx.ConfigValue> valueMap = new HashMap<>();
    valueMap.put(FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_MSP, getOrgMspValue(mspId, msp));

    // Organization's role policy defines what role can perform what operation
    // For example, there are typically four roles policies defined
    // 1. Endorsement
    // 2. Admin
    // 3. Readers
    // 4. Writers
    // The policy type used would be signature, so that the role
    // binding can be done.
    return Configtx.ConfigGroup.newBuilder()
        .setVersion(EMPTY_VERSION)
        .putAllGroups(new HashMap<>())
        .setModPolicy(EMPTY_MOD_POLICY)
        .putAllPolicies(
            FabricChannelUtil.getDefaultRolePolicy(mspId)) // Organization's role policies
        .putAllValues(valueMap)
        .build();
  }

  private Configtx.ConfigValue getOrgMspValue(String mspId, MSPDTO msp) {
    return Configtx.ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setValue(getMspConfig(mspId, msp).toByteString())
        .build();
  }

  private MspConfigPackage.MSPConfig getMspConfig(String mspId, MSPDTO msp) {
    return MspConfigPackage.MSPConfig.newBuilder()
        .setType(0)
        .setConfig(getFabricMSPConfig(mspId, msp).toByteString())
        .build();
  }

  private MspConfigPackage.FabricMSPConfig getFabricMSPConfig(String mspId, MSPDTO msp) {

    List<ByteString> rootCertCollection = new ArrayList<>();
    List<ByteString> tlsRootCertCollection = new ArrayList<>();
    byte[] adminCert;
    byte[] clientCert;
    byte[] peerCert;

    for (String rootCerts : msp.getRootCerts()) {
      rootCertCollection.add(ByteString.copyFrom(rootCerts.getBytes()));
    }
    for (String tlsRootCerts : msp.getTlsRootCerts()) {
      tlsRootCertCollection.add(ByteString.copyFrom(tlsRootCerts.getBytes()));
    }
    adminCert = msp.getAdminOUCert().getBytes();
    clientCert = msp.getClientOUCert().getBytes();
    peerCert = msp.getPeerOUCert().getBytes();

    return MspConfigPackage.FabricMSPConfig.newBuilder()
        .setCryptoConfig(
            MspConfigPackage.FabricCryptoConfig.newBuilder()
                .setIdentityIdentifierHashFunction(
                    FabricClientConstants.CHANNEL_CONFIG_IDENTITY_IDENTIFIER_SHA256)
                .setSignatureHashFamily(
                    FabricClientConstants.CHANNEL_CONFIG_SIGNATURE_HASH_FAMILY_SHA2)
                .build())
        .setFabricNodeOus(
            MspConfigPackage.FabricNodeOUs.newBuilder()
                .setAdminOuIdentifier(
                    MspConfigPackage.FabricOUIdentifier.newBuilder()
                        .setOrganizationalUnitIdentifier(
                            FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ADMIN)
                        .setCertificate(ByteString.copyFrom(adminCert)))
                .setClientOuIdentifier(
                    MspConfigPackage.FabricOUIdentifier.newBuilder()
                        .setOrganizationalUnitIdentifier(
                            FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_CLIENT)
                        .setCertificate(ByteString.copyFrom(clientCert)))
                .setPeerOuIdentifier(
                    MspConfigPackage.FabricOUIdentifier.newBuilder()
                        .setOrganizationalUnitIdentifier(
                            FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_PEER)
                        .setCertificate(ByteString.copyFrom(peerCert)))
                .setEnable(true))
        .setName(mspId)
        .addAllRootCerts(rootCertCollection)
        .addAllTlsRootCerts(tlsRootCertCollection)
        .build();
  }

  /**
   * get consortium config
   *
   * @param consortiumName name of the consortium where channel is created
   * @return consortium config
   */
  private Configtx.ConfigValue getConsortium(String consortiumName) {
    return Configtx.ConfigValue.newBuilder()
        .setVersion(EMPTY_VERSION)
        .setValue(
            Configuration.Consortium.newBuilder().setName(consortiumName).build().toByteString())
        .build();
  }

  /**
   * add default policies to the config group
   *
   * @param builder config group builder
   */
  private void addDefaultImplicitMetaPolicy(Configtx.ConfigGroup.Builder builder) {
    builder.putPolicies(
        "Admins",
        getDefaultConfigPolicy("Admins", Policies.ImplicitMetaPolicy.Rule.MAJORITY_VALUE));
    builder.putPolicies(
        "Writers", getDefaultConfigPolicy("Writers", Policies.ImplicitMetaPolicy.Rule.ANY_VALUE));
    builder.putPolicies(
        "Readers", getDefaultConfigPolicy("Readers", Policies.ImplicitMetaPolicy.Rule.ANY_VALUE));
    builder.putPolicies(
        "Endorsement",
        getDefaultConfigPolicy("Endorsement", Policies.ImplicitMetaPolicy.Rule.MAJORITY_VALUE));
    builder.putPolicies(
        "LifecycleEndorsement",
        getDefaultConfigPolicy("Endorsement", Policies.ImplicitMetaPolicy.Rule.MAJORITY_VALUE));
  }

  /**
   * get implicit meta policy
   *
   * @param subPolicyName what is this policy for? is it an admin, writer, reader
   * @param rule whether majority or any signature
   * @return configuration policy
   */
  private Policies.Policy getImplicitMetaPolicy(String subPolicyName, int rule) {
    Policies.ImplicitMetaPolicy metaPolicy =
        Policies.ImplicitMetaPolicy.newBuilder()
            .setRule(Policies.ImplicitMetaPolicy.Rule.forNumber(rule))
            .setSubPolicy(subPolicyName)
            .build();
    return Policies.Policy.newBuilder()
        .setType(Policies.Policy.PolicyType.IMPLICIT_META_VALUE)
        .setValue(metaPolicy.toByteString())
        .build();
  }

  /**
   * @param subPolicyName what is this policy for? is it an admin, writer, reader
   * @param rule whether majority or any signature
   * @return configuration policy
   */
  private Configtx.ConfigPolicy getDefaultConfigPolicy(String subPolicyName, int rule) {
    return Configtx.ConfigPolicy.newBuilder()
        .setPolicy(getImplicitMetaPolicy(subPolicyName, rule))
        .setModPolicy(DEFAULT_MOD_POLICY)
        .build();
  }

  /**
   * validate the request
   *
   * @param channelOperationRequest input parameters from the API
   * @param channelOperationType whether interested in creation/joining etc.
   * @return status code 0 if it's valid, otherwise return 400
   */
  private ErrorCode validateRequest(
      ChannelOperationRequest channelOperationRequest, ChannelOperationType channelOperationType) {
    if (StringUtils.isEmpty(channelOperationRequest.getChannelName())) {
      return ErrorCode.CHANNEL_NAME_MISSING;
    }
    if (ChannelOperationType.CREATE.equals(channelOperationType)
        && StringUtils.isEmpty(channelOperationRequest.getConsortiumName())) {
      return ErrorCode.CHANNEL_CREATION_MISSING_CONSORTIUM_NAME;
    }
    if (CollectionUtils.isEmpty(channelOperationRequest.getOrderers())) {
      return ErrorCode.CHANNEL_CREATION_MISSING_ORDERER;
    }
    if (ChannelOperationType.JOIN_PEER.equals(channelOperationType)
        && CollectionUtils.isEmpty(channelOperationRequest.getPeers())) {
      return ErrorCode.CHANNEL_CREATION_MISSING_PEER;
    }
    for (hlf.java.rest.client.model.Peer peer : channelOperationRequest.getPeers()) {
      if (StringUtils.isEmpty(peer.getCertificate())) {
        return ErrorCode.CHANNEL_MISSING_CERTIFICATE;
      }
    }
    for (hlf.java.rest.client.model.Orderer orderer : channelOperationRequest.getOrderers()) {
      if (StringUtils.isEmpty(orderer.getCertificate())) {
        return ErrorCode.CHANNEL_MISSING_CERTIFICATE;
      }
    }
    return null;
  }
}
