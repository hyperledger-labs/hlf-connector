package hlf.java.rest.client.service.impl;

import com.google.protobuf.ByteString;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AnchorPeerDTO;
import hlf.java.rest.client.model.ChannelUpdateParamsDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.UpdateChannel;
import hlf.java.rest.client.util.FabricChannelUtil;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
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
public class UpdateChannelImpl implements UpdateChannel {

  private NewOrgParamsDTO newOrganizationDetails;
  private ChannelUpdateParamsDTO channelUpdateParamsDTO;

  @Override
  public ConfigGroup buildWriteset(ConfigGroup readset, ChannelUpdateParamsDTO organizationDetails)
      throws ServiceException {

    newOrganizationDetails = (NewOrgParamsDTO) organizationDetails;
    String newOrgMspId = newOrganizationDetails.getOrganizationMspId();

    Map<String, ConfigGroup> existingOrganizations =
        FabricChannelUtil.getExistingOrgsFromReadset(readset);
    ConfigGroup applicationGroup;

    if (newOrganizationDetails.getMspDTO() != null) {
      // New org addition scenario
      // The "Application" group
      applicationGroup =
          ConfigGroup.newBuilder()
              .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
              .putAllPolicies(FabricChannelUtil.setApplicationPolicies(readset))
              .putGroups(newOrgMspId, setNewOrgGroup(newOrgMspId))
              // putAllGroups excludes new organization
              .putAllGroups(existingOrganizations)
              // Application group version
              .setVersion(
                  FabricChannelUtil.retrieveMSPGroupVersionFromReadset(
                          readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION)
                      + 1) // will be tied to current version + 1 for this level
              .build();
    } else {
      // The "Application" group
      applicationGroup =
          ConfigGroup.newBuilder()
              .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
              .putAllPolicies(FabricChannelUtil.setApplicationPolicies(readset))
              .putGroups(newOrgMspId, setAnchorPeerInGroup(newOrgMspId, readset))
              // putAllGroups excludes new organization
              .putAllGroups(existingOrganizations)
              // Application group version
              .setVersion(
                  FabricChannelUtil.retrieveMSPGroupVersionFromReadset(
                          readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION)
                      + 1) // will be tied to current version + 1 for this level
              .build();
    }

    // the "/Channel" group
    return ConfigGroup.newBuilder()
        .putGroups(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION, applicationGroup)
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        // Channel group version
        .setVersion(readset.getVersion())
        .build();
  }

  private ConfigGroup setAnchorPeerInGroup(String orgMspId, ConfigGroup readSet) {
    Map<String, ConfigValue> valueMap = new HashMap<>();
    if (newOrganizationDetails.getAnchorPeerDTOs() != null) {
      valueMap.put(
          FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_ANCHORPEERS, setNewOrgAnchorPeerValue());
    }
    return ConfigGroup.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .putAllPolicies(FabricChannelUtil.getDefaultRolePolicy(orgMspId))
        .putAllValues(valueMap)
        .setVersion(
            FabricChannelUtil.retrieveMSPGroupVersionFromReadset(
                    readSet, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION)
                + 1)
        .build();
  }

  private ConfigGroup setNewOrgGroup(String newOrgMspId) {
    Map<String, ConfigValue> valueMap = new HashMap<>();
    valueMap.put(
        FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_MSP, setNewOrgMspValue(newOrgMspId));
    if (newOrganizationDetails.getAnchorPeerDTOs() != null) {
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
    for (AnchorPeerDTO anchorPeerDTO : newOrganizationDetails.getAnchorPeerDTOs()) {
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
    byte[] adminCert;
    byte[] clientCert;
    byte[] ordererCert;
    byte[] peerCert;

    for (String rootCerts : newOrganizationDetails.getMspDTO().getRootCerts()) {
      rootCertCollection.add(ByteString.copyFrom(rootCerts.getBytes()));
    }
    for (String tlsRootCerts : newOrganizationDetails.getMspDTO().getTlsRootCerts()) {
      tlsRootCertCollection.add(ByteString.copyFrom(tlsRootCerts.getBytes()));
    }
    adminCert = newOrganizationDetails.getMspDTO().getAdminOUCert().getBytes();
    clientCert = newOrganizationDetails.getMspDTO().getClientOUCert().getBytes();

    FabricNodeOUs.Builder builder = null;
    if (newOrganizationDetails.getMspDTO().getOrdererOUCert() != null) {
      ordererCert = newOrganizationDetails.getMspDTO().getOrdererOUCert().getBytes();
      builder = getFabricNodeOUs(true, adminCert, clientCert, ordererCert);
    }
    if (newOrganizationDetails.getMspDTO().getPeerOUCert() != null) {
      peerCert = newOrganizationDetails.getMspDTO().getPeerOUCert().getBytes();
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
