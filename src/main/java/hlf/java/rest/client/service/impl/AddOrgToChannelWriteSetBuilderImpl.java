package hlf.java.rest.client.service.impl;

import com.google.protobuf.ByteString;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AnchorPeerDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.AddOrgToChannelWriteSetBuilder;
import hlf.java.rest.client.util.FabricChannelUtil;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.common.Configtx.ConfigPolicy;
import org.hyperledger.fabric.protos.common.Configtx.ConfigValue;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricCryptoConfig;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricMSPConfig;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricNodeOUs;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricOUIdentifier;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.MSPConfig;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeer;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeers;
import org.springframework.stereotype.Service;

@Service
public class AddOrgToChannelWriteSetBuilderImpl implements AddOrgToChannelWriteSetBuilder {

  private NewOrgParamsDTO organizationDetails;
  private static final int DEFAULT_VERSION = 0;

  @Override
  public ConfigGroup buildWriteset(ConfigGroup readset, NewOrgParamsDTO organizationDetails)
      throws ServiceException {
    this.organizationDetails = organizationDetails;
    String newOrgMspId = organizationDetails.getOrganizationMspId();
    // Get existing organizations in the channel and set with as objects and their
    // version to prevent deletion or modification
    // Omitting existing groups results in their deletion.
    Map<String, ConfigGroup> existingOrganizations = new HashMap<>();
    ConfigGroup applicationConfigGroup =
        readset.getGroupsOrThrow(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    applicationConfigGroup
        .getGroupsMap()
        .forEach(
            (k, v) ->
                existingOrganizations.put(
                    k,
                    setEmptyGroup(retrieveMSPGroupVersionFromReadset(applicationConfigGroup, k))));
    // The "Application" group
    ConfigGroup applicationGroup =
        ConfigGroup.newBuilder()
            .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
            .putAllPolicies(setApplicationPolicies(readset))
            .putGroups(newOrgMspId, setNewOrgGroup(newOrgMspId))
            // putAllGroups excludes new organization
            .putAllGroups(existingOrganizations)
            // Application group version
            .setVersion(
                retrieveMSPGroupVersionFromReadset(
                        readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION)
                    + 1) // will
            // be
            // tied
            // to
            // current
            // version
            // + 1
            // for
            // this
            // level
            .build();
    // the "/Channel" group
    return ConfigGroup.newBuilder()
        .putGroups(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION, applicationGroup)
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        // Channel group version
        .setVersion(readset.getVersion())
        .build();
  }

  private long retrieveMSPGroupVersionFromReadset(ConfigGroup readset, String mspId)
      throws ServiceException {
    long versionLong = DEFAULT_VERSION;
    try {
      ConfigGroup group = readset.getGroupsOrThrow(mspId);
      versionLong = group.getVersion();
    } catch (IllegalArgumentException e) {
      throw new ServiceException(
          ErrorCode.NOT_FOUND,
          "WriteBuilder version iteration error: ConfigGroup with name - \""
              + mspId
              + "\" - not found in Readset",
          e);
    }
    return versionLong;
  }

  private Map<String, Long> retrievePolicyVersionFromReadset(ConfigGroup readset, String groupName)
      throws ServiceException {
    Map<String, Long> map = new HashMap<>();
    try {
      ConfigGroup group = readset.getGroupsOrThrow(groupName);
      for (Entry<String, ConfigPolicy> entry : group.getPoliciesMap().entrySet()) {
        map.put(entry.getKey(), entry.getValue().getVersion());
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceException(
          ErrorCode.NOT_FOUND,
          "WriteBuilder version iteration error: ConfigGroup with name - \""
              + groupName
              + "\" - not found in Readset",
          e);
    }
    return map;
  }

  private Map<String, ConfigPolicy> setApplicationPolicies(ConfigGroup readset) {
    Map<String, Long> map =
        retrievePolicyVersionFromReadset(
            readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    ConfigPolicy adminPolicy =
        ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS))
            .build();
    ConfigPolicy endorsementPolicy =
        ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT))
            .build();
    ConfigPolicy lifeCycleEndorsementPolicy =
        ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(
                map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT))
            .build();
    ConfigPolicy readerPolicy =
        ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS))
            .build();
    ConfigPolicy writerPolicy =
        ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS))
            .build();
    Map<String, ConfigPolicy> applicationPoliciesMap = new HashMap<>();
    // add Admins, Readers, Writers, Endorsement and LifeCycle Endorsement policies at the channel
    // level
    applicationPoliciesMap.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS, adminPolicy);
    applicationPoliciesMap.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT, endorsementPolicy);
    applicationPoliciesMap.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT,
        lifeCycleEndorsementPolicy);
    applicationPoliciesMap.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS, readerPolicy);
    applicationPoliciesMap.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS, writerPolicy);
    return applicationPoliciesMap;
  }

  private ConfigGroup setNewOrgGroup(String newOrgMspId) {
    Map<String, ConfigValue> valueMap = new HashMap<>();
    valueMap.put(
        FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_MSP, setNewOrgMspValue(newOrgMspId));
    if (organizationDetails.getAnchorPeerDTOs() != null) {
      valueMap.put(
          FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_ANCHORPEERS, setNewOrgAnchorPeerValue());
    }

    return ConfigGroup.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .putAllPolicies(FabricChannelUtil.getDefaultRolePolicy(newOrgMspId))
        .putAllValues(valueMap)
        .setVersion(0) // First time update, hence version is 0
        .build();
  }

  private ConfigGroup setEmptyGroup(long version) {
    return ConfigGroup.newBuilder().setModPolicy("").setVersion(version).build();
  }

  private ConfigValue setNewOrgMspValue(String newOrgMspId) {
    return ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setValue(
            setMspConfig(newOrgMspId)
                .toByteString()) // ByteString, need to figure out how to build the proper
        // structure
        .setVersion(0)
        .build();
  }

  private MSPConfig setMspConfig(String newOrgMspId) {
    return MSPConfig.newBuilder()
        .setType(0)
        .setConfig(newOrgValue(newOrgMspId).toByteString())
        .build();
  }

  private ConfigValue setNewOrgAnchorPeerValue() {
    return ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setValue(setAnchorPeers().toByteString())
        .setVersion(0)
        .build();
  }

  private AnchorPeers setAnchorPeers() {
    List<AnchorPeer> anchorPeerList = new ArrayList<>();
    for (AnchorPeerDTO anchorPeerDTO : organizationDetails.getAnchorPeerDTOs()) {
      anchorPeerList.add(
          AnchorPeer.newBuilder()
              .setHost(anchorPeerDTO.getHostname())
              .setPort(anchorPeerDTO.getPort())
              .build());
    }
    return AnchorPeers.newBuilder().addAllAnchorPeers(anchorPeerList).build();
  }

  // Error with this section when converting block.pb from PROTO to JSONBtye
  private FabricMSPConfig newOrgValue(String newOrgMspId) {
    // MSP cacerts full certificate (including the ----BEGIN... and ----END...
    // tags), NOT base64 as that's done by fabric on commit
    List<ByteString> rootCertCollection = new ArrayList<>();
    List<ByteString> tlsRootCertCollection = new ArrayList<>();
    byte[] adminCert = null;
    byte[] clientCert = null;
    byte[] ordererCert = null;
    byte[] peerCert = null;

    for (String rootCerts : organizationDetails.getMspDTO().getRootCerts()) {
      rootCertCollection.add(ByteString.copyFrom(rootCerts.getBytes()));
    }
    for (String tlsRootCerts : organizationDetails.getMspDTO().getTlsRootCerts()) {
      tlsRootCertCollection.add(ByteString.copyFrom(tlsRootCerts.getBytes()));
    }
    adminCert = organizationDetails.getMspDTO().getAdminOUCert().getBytes();
    clientCert = organizationDetails.getMspDTO().getClientOUCert().getBytes();

    FabricNodeOUs.Builder builder = null;
    if (organizationDetails.getMspDTO().getOrdererOUCert() != null) {
      ordererCert = organizationDetails.getMspDTO().getOrdererOUCert().getBytes();
      builder = getFabricNodeOUs(true, adminCert, clientCert, ordererCert);
    }
    if (organizationDetails.getMspDTO().getPeerOUCert() != null) {
      peerCert = organizationDetails.getMspDTO().getPeerOUCert().getBytes();
      builder = getFabricNodeOUs(false, adminCert, clientCert, peerCert);
    }

    return FabricMSPConfig.newBuilder()
        .setCryptoConfig(
            FabricCryptoConfig.newBuilder()
                .setIdentityIdentifierHashFunction(
                    FabricClientConstants.CHANNEL_CONFIG_IDENTITY_IDENTIFIER_SHA256)
                .setSignatureHashFamily(
                    FabricClientConstants.CHANNEL_CONFIG_SIGNATURE_HASH_FAMILY_SHA2)
                .build())
        .setFabricNodeOus(builder)
        .setName(newOrgMspId)
        .addAllRootCerts(rootCertCollection)
        .addAllTlsRootCerts(tlsRootCertCollection)
        .build();
  }

  private FabricNodeOUs.Builder getFabricNodeOUs(
      boolean isOrderer, byte[] adminCert, byte[] clientCert, byte[] nodeCert) {
    if (isOrderer) {
      return FabricNodeOUs.newBuilder()
          .setAdminOuIdentifier(
              FabricOUIdentifier.newBuilder()
                  .setOrganizationalUnitIdentifier(
                      FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ADMIN)
                  .setCertificate(ByteString.copyFrom(adminCert)))
          .setClientOuIdentifier(
              FabricOUIdentifier.newBuilder()
                  .setOrganizationalUnitIdentifier(
                      FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_CLIENT)
                  .setCertificate(ByteString.copyFrom(clientCert)))
          .setOrdererOuIdentifier(
              FabricOUIdentifier.newBuilder()
                  .setOrganizationalUnitIdentifier(
                      FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ORDERER)
                  .setCertificate(ByteString.copyFrom(nodeCert)))
          .setEnable(true);
    }
    return FabricNodeOUs.newBuilder()
        .setAdminOuIdentifier(
            FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(
                    FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ADMIN)
                .setCertificate(ByteString.copyFrom(adminCert)))
        .setClientOuIdentifier(
            FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(
                    FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_CLIENT)
                .setCertificate(ByteString.copyFrom(clientCert)))
        .setPeerOuIdentifier(
            FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(
                    FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_PEER)
                .setCertificate(ByteString.copyFrom(nodeCert)))
        .setEnable(true);
  }
}
