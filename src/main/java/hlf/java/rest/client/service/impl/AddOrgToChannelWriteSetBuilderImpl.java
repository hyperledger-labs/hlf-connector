package hlf.java.rest.client.service.impl;

import com.google.protobuf.ByteString;
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
public class AddOrgToChannelWriteSetBuilderImpl implements AddOrgToChannelWriteSetBuilder {

  private NewOrgParamsDTO organizationDetails;

  @Override
  public ConfigGroup buildWriteset(ConfigGroup readset, NewOrgParamsDTO organizationDetails)
      throws ServiceException {
    this.organizationDetails = organizationDetails;
    String newOrgMspId = organizationDetails.getOrganizationMspId();
    Map<String, ConfigGroup> existingOrganizations =
        FabricChannelUtil.getExistingOrgsFromReadset(readset);

    // The "Application" group
    ConfigGroup applicationGroup =
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
    // the "/Channel" group
    return ConfigGroup.newBuilder()
        .putGroups(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION, applicationGroup)
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        // Channel group version
        .setVersion(readset.getVersion())
        .build();
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
