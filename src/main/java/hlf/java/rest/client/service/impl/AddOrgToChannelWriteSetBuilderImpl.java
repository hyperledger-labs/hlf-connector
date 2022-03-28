package hlf.java.rest.client.service.impl;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.common.Configtx.ConfigPolicy;
import org.hyperledger.fabric.protos.common.Configtx.ConfigValue;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPPrincipal;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPRole;
import org.hyperledger.fabric.protos.common.Policies.Policy;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicy;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicy.NOutOf;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicyEnvelope;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricCryptoConfig;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricMSPConfig;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricNodeOUs;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricOUIdentifier;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.MSPConfig;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeer;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeers;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AnchorPeerDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.AddOrgToChannelWriteSetBuilder;
import hlf.java.rest.client.util.FabricClientConstants;

@Service
public class AddOrgToChannelWriteSetBuilderImpl implements AddOrgToChannelWriteSetBuilder {

  private NewOrgParamsDTO organizationDetails;
  private static final int DEFAULT_VERSION = 0;

  @Override
  public ConfigGroup buildWriteset(ConfigGroup readset, NewOrgParamsDTO organizationDetails) throws ServiceException {
    this.organizationDetails = organizationDetails;
    String newOrgName = organizationDetails.getOrganizationName();
    // Get existing organizations in the channel and set with as objects and their
    // version to prevent deletion or modification
    // Omitting existing groups results in their deletion.
    Map<String, ConfigGroup> organizations = new HashMap<String, ConfigGroup>();
    ConfigGroup applicationConfigGroup = readset
        .getGroupsOrThrow(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    applicationConfigGroup.getGroupsMap().forEach((k, v) -> {
      organizations.put(k, setEmptyGroup(retrieveGroupVersionFromReadset(applicationConfigGroup, k)));
    });
    // The "Application" group
    ConfigGroup applicationGroup = ConfigGroup.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .putAllPolicies(setApplicationPolicies(readset)).putGroups(newOrgName, setNewOrgGroup(newOrgName))
        .putAllGroups(organizations)
        // Application group version
        .setVersion(
            retrieveGroupVersionFromReadset(readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION) + 1) // will
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
    ConfigGroup writeset = ConfigGroup.newBuilder()
        .putGroups(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION, applicationGroup)
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        // Channel group version
        .setVersion(readset.getVersion()).build();
    return writeset;
  }

  private long retrieveGroupVersionFromReadset(ConfigGroup readset, String groupName) throws ServiceException {
    long versionLong = DEFAULT_VERSION;
    try {
      ConfigGroup group = readset.getGroupsOrThrow(groupName);
      versionLong = group.getVersion();
    } catch (IllegalArgumentException e) {
      throw new ServiceException(ErrorCode.NOT_FOUND,
          "WriteBuilder version iteration error: ConfigGroup with name - \"" + groupName + "\" - not found in Readset",
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
      throw new ServiceException(ErrorCode.NOT_FOUND,
          "WriteBuilder version iteration error: ConfigGroup with name - \"" + groupName + "\" - not found in Readset",
          e);
    }
    return map;
  }

  private Map<String, ConfigPolicy> setApplicationPolicies(ConfigGroup readset) {
    Map<String, Long> map = retrievePolicyVersionFromReadset(readset,
        FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    ConfigPolicy adminPolicy = ConfigPolicy.newBuilder().setModPolicy("")
        .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS)).build();
    ConfigPolicy endorsementPolicy = ConfigPolicy.newBuilder().setModPolicy("")
        .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT)).build();
    ConfigPolicy lifeCycleEndorsementPolicy = ConfigPolicy.newBuilder().setModPolicy("")
        .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT)).build();
    ConfigPolicy readerPolicy = ConfigPolicy.newBuilder().setModPolicy("")
        .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS)).build();
    ConfigPolicy writerPolicy = ConfigPolicy.newBuilder().setModPolicy("")
        .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS)).build();

    Map<String, ConfigPolicy> applicationPoliciesMap = new HashMap<String, ConfigPolicy>();
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS, adminPolicy);
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT, endorsementPolicy);
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT,
        lifeCycleEndorsementPolicy);
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS, readerPolicy);
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS, writerPolicy);
    return applicationPoliciesMap;
  }

  private ConfigGroup setNewOrgGroup(String newOrgName) {
    Map<String, ConfigValue> valueMap = new HashMap<String, ConfigValue>();
    valueMap.put(FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_MSP, setNewOrgMspValue(newOrgName));
    if(organizationDetails.getAnchorPeerDTOs() != null) {
      valueMap.put(FabricClientConstants.CHANNEL_CONFIG_GROUP_VALUE_ANCHORPEERS, setNewOrgAnchorPeerValue(newOrgName));
    }

    ConfigGroup newOrgGroup = ConfigGroup.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .putAllPolicies(setNewOrgPolicies(newOrgName)).putAllValues(valueMap).setVersion(0).build();
    return newOrgGroup;
  }

  private ConfigGroup setEmptyGroup(long version) {
    ConfigGroup newOrgGroup = ConfigGroup.newBuilder().setModPolicy("").setVersion(version).build();
    return newOrgGroup;
  }

  private Map<String, ConfigPolicy> setNewOrgPolicies(String newOrgName) {
    Map<String, ConfigPolicy> applicationPoliciesMap = new HashMap<String, ConfigPolicy>();
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS,
        setNewOrgPolicy(newOrgName, FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS));
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT,
        setNewOrgPolicy(newOrgName, FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT));
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS,
        setNewOrgPolicy(newOrgName, FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS));
    applicationPoliciesMap.put(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS,
        setNewOrgPolicy(newOrgName, FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS));

    return applicationPoliciesMap;
  }

  private ConfigPolicy setNewOrgPolicy(String newOrgName, String policyTarget) {
    ConfigPolicy appPolicy = ConfigPolicy.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setPolicy(setTypeOnePolicy(newOrgName, policyTarget)).setVersion(0).build();
    return appPolicy;
  }

  private Policy setTypeOnePolicy(String orgName, String policyTarget) {
    ArrayList<MSPPrincipal> identitiesList = new ArrayList<MSPPrincipal>();

    MSPRole mspRoleAdmin = MSPRole.newBuilder().setRole(MSPRole.MSPRoleType.ADMIN).setMspIdentifier(orgName).build();
    MSPPrincipal mspPrincipalAdmin = MSPPrincipal.newBuilder().setPrincipal(mspRoleAdmin.toByteString())
        .setPrincipalClassification(MSPPrincipal.Classification.ROLE).build();
    MSPRole mspRolePeer = MSPRole.newBuilder().setRole(MSPRole.MSPRoleType.PEER).setMspIdentifier(orgName).build();
    MSPPrincipal mspPrincipalPeer = MSPPrincipal.newBuilder().setPrincipal(mspRolePeer.toByteString())
        .setPrincipalClassification(MSPPrincipal.Classification.ROLE).build();
    MSPRole mspRoleClient = MSPRole.newBuilder().setRole(MSPRole.MSPRoleType.CLIENT).setMspIdentifier(orgName).build();
    MSPPrincipal mspPrincipalClient = MSPPrincipal.newBuilder().setPrincipal(mspRoleClient.toByteString())
        .setPrincipalClassification(MSPPrincipal.Classification.ROLE).build();

    // "SignaturePolicy" is repeated internally despite being the same class, but
    // with
    // different internal components used
    SignaturePolicy rules = null;
    NOutOf nOutOf = null;

    switch (policyTarget) {
    case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS:
      identitiesList.add(mspPrincipalAdmin);
      rules = SignaturePolicy.newBuilder().setSignedBy(0).build();
      nOutOf = NOutOf.newBuilder().setN(1).addRules(rules).build();
      break;
    case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT:
      identitiesList.add(mspPrincipalPeer);
      rules = SignaturePolicy.newBuilder().setSignedBy(0).build();
      nOutOf = NOutOf.newBuilder().setN(1).addRules(rules).build();
      break;
    case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS:
      identitiesList.add(mspPrincipalAdmin);
      identitiesList.add(mspPrincipalPeer);
      identitiesList.add(mspPrincipalClient);
      rules = SignaturePolicy.newBuilder().setSignedBy(0).setSignedBy(1).setSignedBy(2).build();
      nOutOf = NOutOf.newBuilder().setN(3).addRules(rules).build();
      break;
    case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS:
      identitiesList.add(mspPrincipalAdmin);
      identitiesList.add(mspPrincipalClient);
      rules = SignaturePolicy.newBuilder().setSignedBy(0).setSignedBy(1).build();
      nOutOf = NOutOf.newBuilder().setN(2).addRules(rules).build();
      break;
    case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT:
      // Fill later based on requirements
      break;
    default:
      throw new ServiceException(ErrorCode.NOT_FOUND,
          "Error building readset. Policy Type: \"" + policyTarget + "\" not found.");
    }
    SignaturePolicy rule = SignaturePolicy.newBuilder().setNOutOf(nOutOf).build();

    // For type 1 policy
    SignaturePolicyEnvelope spe = SignaturePolicyEnvelope.newBuilder().setVersion(0).addAllIdentities(identitiesList)
        .setRule(rule).build();

    Policy policy = Policy.newBuilder().setType(1).setValue(spe.toByteString()).build();
    return policy;
  }

  private ConfigValue setNewOrgMspValue(String newOrgName) {
    ConfigValue configValue = ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setValue(setMspConfig(newOrgName).toByteString()) // ByteString, need to figure out how to build the proper
                                                           // structure
        .setVersion(0).build();
    return configValue;
  }

  private MSPConfig setMspConfig(String newOrgName) {
    return MSPConfig.newBuilder().setType(0).setConfig(newOrgValue(newOrgName).toByteString()).build();
  }

  private ConfigValue setNewOrgAnchorPeerValue(String newOrgName) {
    ConfigValue configValue = ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS).setValue(setAnchorPeers().toByteString())
        .setVersion(0).build();
    return configValue;
  }

  private AnchorPeers setAnchorPeers() {
    List<AnchorPeer> anchorPeerList = new ArrayList<AnchorPeer>();
    for (AnchorPeerDTO anchorPeerDTO : organizationDetails.getAnchorPeerDTOs()) {
      anchorPeerList
          .add(AnchorPeer.newBuilder().setHost(anchorPeerDTO.getHostname()).setPort(anchorPeerDTO.getPort()).build());
    }
    return AnchorPeers.newBuilder().addAllAnchorPeers(anchorPeerList).build();
  }

  // Error with this section when converting block.pb from PROTO to JSONBtye
  private FabricMSPConfig newOrgValue(String newOrgName) {
    // MSP cacerts full certificate (including the ----BEGIN... and ----END...
    // tags), NOT base64 as that's done by fabric on commit
    List<ByteString> rootCertCollection = new ArrayList<ByteString>();
    List<ByteString> tlsRootCertCollection = new ArrayList<ByteString>();
    byte[] adminCert = null;
    byte[] clientCert = null;
    byte[] ordererCert = null;
    byte[] peerCert = null;

    for (String rootCerts : organizationDetails.getMspDTO().getRootCerts()) {
      rootCertCollection.add(ByteString.copyFrom(Base64.getDecoder().decode(rootCerts)));
    }
    for (String tlsRootCerts : organizationDetails.getMspDTO().getTlsRootCerts()) {
      tlsRootCertCollection.add(ByteString.copyFrom(Base64.getDecoder().decode(tlsRootCerts)));
    }
    adminCert = Base64.getDecoder().decode(organizationDetails.getMspDTO().getAdminOUCert());
    clientCert = Base64.getDecoder().decode(organizationDetails.getMspDTO().getClientOUCert());
    ordererCert = Base64.getDecoder().decode(organizationDetails.getMspDTO().getOrdererOUCert());
    peerCert = Base64.getDecoder().decode(organizationDetails.getMspDTO().getPeerOUCert());

    FabricMSPConfig builder = FabricMSPConfig.newBuilder()
        .setCryptoConfig(FabricCryptoConfig.newBuilder()
            .setIdentityIdentifierHashFunction(FabricClientConstants.CHANNEL_CONFIG_IDENTITY_IDENTIFIER_SHA256)
            .setSignatureHashFamily(FabricClientConstants.CHANNEL_CONFIG_SIGNATURE_HASH_FAMILY_SHA2).build())
        .setFabricNodeOus(FabricNodeOUs.newBuilder()
            .setAdminOuIdentifier(FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ADMIN)
                .setCertificate(ByteString.copyFrom(adminCert)))
            .setClientOuIdentifier(FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_CLIENT)
                .setCertificate(ByteString.copyFrom(clientCert)))
            .setOrdererOuIdentifier(FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ORDERER)
                .setCertificate(ByteString.copyFrom(ordererCert)))
            .setPeerOuIdentifier(FabricOUIdentifier.newBuilder()
                .setOrganizationalUnitIdentifier(FabricClientConstants.CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_PEER)
                .setCertificate(ByteString.copyFrom(peerCert)))
            .setEnable(true))
        .setName(newOrgName).addAllRootCerts(rootCertCollection).addAllTlsRootCerts(tlsRootCertCollection).build();
    return builder;
  }

}