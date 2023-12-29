package hlf.java.rest.client.util;

import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.hyperledger.fabric.protos.common.Configtx;
import org.hyperledger.fabric.protos.common.MspPrincipal;
import org.hyperledger.fabric.protos.common.Policies;

@UtilityClass
public class FabricChannelUtil {
  private final int DEFAULT_VERSION = 0;

  /**
   * get default configuration policy for organization that maps the roles. The policy type is
   * signature. Roles are identified by their signatures, as those signatures represent the
   * certificate.
   *
   * @param orgMSPId Org MSP ID
   * @return HashMap with role and the configuration policy
   */
  public HashMap<String, Configtx.ConfigPolicy> getDefaultRolePolicy(String orgMSPId) {
    HashMap<String, Configtx.ConfigPolicy> defaultOrgRolePolicy = new HashMap<>();
    // add Admins, Readers, Writers and Endorsement policies
    defaultOrgRolePolicy.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS,
        getDefaultRoleConfigPolicyForMSP(
            FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS, orgMSPId));
    defaultOrgRolePolicy.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS,
        getDefaultRoleConfigPolicyForMSP(
            FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS, orgMSPId));
    defaultOrgRolePolicy.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS,
        getDefaultRoleConfigPolicyForMSP(
            FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS, orgMSPId));
    defaultOrgRolePolicy.put(
        FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT,
        getDefaultRoleConfigPolicyForMSP(
            FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT, orgMSPId));
    return defaultOrgRolePolicy;
  }

  /**
   * returns a ConfigPolicy of type signature for the passed organization's MSP ID
   *
   * @param policyFor Policy for which role
   * @param orgMSPId new org MSP ID
   * @return configuration policy
   */
  private Configtx.ConfigPolicy getDefaultRoleConfigPolicyForMSP(
      String policyFor, String orgMSPId) {
    List<MspPrincipal.MSPPrincipal> mspPrincipals = getRolesFor(policyFor, orgMSPId);
    // loop through each entry and apply the n out of policy
    // that is always get at least one signature.
    // get the signature policy
    // set rules
    // create those roles
    Policies.SignaturePolicyEnvelope.Builder signaturePolicyEnvelopeBuilder =
        Policies.SignaturePolicyEnvelope.newBuilder().addAllIdentities(mspPrincipals);
    Policies.SignaturePolicy.Builder signaturePolicyBuilder = Policies.SignaturePolicy.newBuilder();
    Policies.SignaturePolicy.NOutOf.Builder signatureNOutOfBuilder =
        Policies.SignaturePolicy.NOutOf.newBuilder().setN(1); // expect just one signature always
    List<Policies.SignaturePolicy> signaturePolicies = new ArrayList<>();
    for (int idx = 0; idx < mspPrincipals.size(); idx++) {
      signaturePolicies.add(Policies.SignaturePolicy.newBuilder().setSignedBy(idx).build());
    }
    signatureNOutOfBuilder.addAllRules(signaturePolicies);
    signaturePolicyBuilder.setNOutOf(signatureNOutOfBuilder.build());
    signaturePolicyEnvelopeBuilder.setRule(signaturePolicyBuilder.build());
    // get the policy
    Policies.Policy policy =
        Policies.Policy.newBuilder()
            .setType(Policies.Policy.PolicyType.SIGNATURE_VALUE)
            .setValue(signaturePolicyEnvelopeBuilder.build().toByteString())
            .build();
    // create config policy and return
    return Configtx.ConfigPolicy.newBuilder()
        .setPolicy(policy)
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .build();
  }

  // getRolesFor returns the SignaturePolicy that has MSP
  // with the logical conditions.
  // For example, it is possible to design OR(msp1.member, msp2.client)
  // this evaluates to
  // identities: {
  //  ... msp1
  //  ... msp2
  // }
  // n out of {
  //   n: 1
  //   rules: {
  //      SignaturePolicy{index: 0}
  //      SignaturePolicy{index: 1}
  //   }
  // }

  private MspPrincipal.MSPPrincipal createMSPPrincipal(
      String orgMSPId, MspPrincipal.MSPRole.MSPRoleType roleType) {
    MspPrincipal.MSPRole mspRole =
        MspPrincipal.MSPRole.newBuilder().setMspIdentifier(orgMSPId).setRole(roleType).build();
    return MspPrincipal.MSPPrincipal.newBuilder()
        .setPrincipal(mspRole.toByteString())
        .setPrincipalClassification(MspPrincipal.MSPPrincipal.Classification.ROLE)
        .build();
  }

  private List<MspPrincipal.MSPPrincipal> getRolesFor(String policyFor, String orgMSPId) {
    List<MspPrincipal.MSPPrincipal> mspPrincipals = new ArrayList<>();
    switch (policyFor) {
      case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS:
        mspPrincipals.add(createMSPPrincipal(orgMSPId, MspPrincipal.MSPRole.MSPRoleType.ADMIN));
        break;
      case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS:
        // any member who is an admin can write
        mspPrincipals.add(createMSPPrincipal(orgMSPId, MspPrincipal.MSPRole.MSPRoleType.ADMIN));
        // any client can also write
        mspPrincipals.add(createMSPPrincipal(orgMSPId, MspPrincipal.MSPRole.MSPRoleType.CLIENT));
        break;
      case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT:
        // any member who is peer can only endorse
        mspPrincipals.add(createMSPPrincipal(orgMSPId, MspPrincipal.MSPRole.MSPRoleType.PEER));
        break;
      case FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS:
        // any member can read
        mspPrincipals.add(createMSPPrincipal(orgMSPId, MspPrincipal.MSPRole.MSPRoleType.MEMBER));
        break;
    }
    return mspPrincipals;
  }

  /* Get existing organizations in the channel and set with as objects and their
  version to prevent deletion or modification.
  Omitting existing groups results in their deletion.*/
  public Map<String, Configtx.ConfigGroup> getExistingOrgsFromReadset(
      Configtx.ConfigGroup readset) {
    Map<String, Configtx.ConfigGroup> existingOrganizations = new HashMap<>();
    Configtx.ConfigGroup applicationConfigGroup =
        readset.getGroupsOrThrow(FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    applicationConfigGroup
        .getGroupsMap()
        .forEach(
            (k, v) ->
                existingOrganizations.put(
                    k,
                    setEmptyGroup(retrieveMSPGroupVersionFromReadset(applicationConfigGroup, k))));

    return existingOrganizations;
  }

  private Configtx.ConfigGroup setEmptyGroup(long version) {
    return Configtx.ConfigGroup.newBuilder().setModPolicy("").setVersion(version).build();
  }

  public long retrieveMSPGroupVersionFromReadset(Configtx.ConfigGroup readset, String mspId)
      throws ServiceException {
    long versionLong = DEFAULT_VERSION;
    try {
      Configtx.ConfigGroup group = readset.getGroupsOrThrow(mspId);
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

  private Map<String, Long> retrievePolicyVersionFromReadset(
      Configtx.ConfigGroup readset, String groupName) throws ServiceException {
    Map<String, Long> map = new HashMap<>();
    try {
      Configtx.ConfigGroup group = readset.getGroupsOrThrow(groupName);
      for (Map.Entry<String, Configtx.ConfigPolicy> entry : group.getPoliciesMap().entrySet()) {
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

  public Map<String, Configtx.ConfigPolicy> setApplicationPolicies(Configtx.ConfigGroup readset) {
    Map<String, Long> map =
        retrievePolicyVersionFromReadset(
            readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION);
    Configtx.ConfigPolicy adminPolicy =
        Configtx.ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ADMINS))
            .build();
    Configtx.ConfigPolicy endorsementPolicy =
        Configtx.ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT))
            .build();
    Configtx.ConfigPolicy lifeCycleEndorsementPolicy =
        Configtx.ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(
                map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT))
            .build();
    Configtx.ConfigPolicy readerPolicy =
        Configtx.ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_READERS))
            .build();
    Configtx.ConfigPolicy writerPolicy =
        Configtx.ConfigPolicy.newBuilder()
            .setModPolicy("")
            .setVersion(map.get(FabricClientConstants.CHANNEL_CONFIG_POLICY_TYPE_WRITERS))
            .build();
    Map<String, Configtx.ConfigPolicy> applicationPoliciesMap = new HashMap<>();
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
}
