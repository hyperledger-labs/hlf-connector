package hlf.java.rest.client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.hyperledger.fabric.protos.common.Configtx;
import org.hyperledger.fabric.protos.common.MspPrincipal;
import org.hyperledger.fabric.protos.common.Policies;

@UtilityClass
public class FabricChannelUtil {

  /**
   * get default configuration policy for organization that maps the roles. The policy type is
   * signature. Roles are identified by their signatures, as those signatures represent the
   * certificate.
   *
   * @param orgMSPId Org MSP ID
   * @return HashMap with role and the configuration policy
   */
  public static HashMap<String, Configtx.ConfigPolicy> getDefaultRolePolicy(String orgMSPId) {
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
  private static Configtx.ConfigPolicy getDefaultRoleConfigPolicyForMSP(
      String policyFor, String orgMSPId) {
    List<MspPrincipal.MSPPrincipal> mspPrincipals = getRolesFor(policyFor, orgMSPId);
    // loop through each entry and apply the n out of policy
    // that is always get at least one signature.
    // get the signature policy
    // set rules
    // create those roles
    Policies.SignaturePolicyEnvelope.Builder signaturePolicyEnvelopeBuilder =
        Policies.SignaturePolicyEnvelope.newBuilder();
    Policies.SignaturePolicy.Builder signaturePolicyBuilder = Policies.SignaturePolicy.newBuilder();
    Policies.SignaturePolicy.NOutOf.Builder signatureNOutOfBuilder =
        Policies.SignaturePolicy.NOutOf.newBuilder().setN(1); // expect just one signature always
    for (int idx = 0; idx < mspPrincipals.size(); idx++) {
      signaturePolicyEnvelopeBuilder.setIdentities(idx, mspPrincipals.get(idx));
      signatureNOutOfBuilder.setRules(
          idx, Policies.SignaturePolicy.newBuilder().setSignedBy(idx).build());
    }
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

  private static MspPrincipal.MSPPrincipal createMSPPrincipal(
      String orgMSPId, MspPrincipal.MSPRole.MSPRoleType roleType) {
    MspPrincipal.MSPRole mspRole =
        MspPrincipal.MSPRole.newBuilder().setMspIdentifier(orgMSPId).setRole(roleType).build();
    return MspPrincipal.MSPPrincipal.newBuilder()
        .setPrincipal(mspRole.toByteString())
        .setPrincipalClassification(MspPrincipal.MSPPrincipal.Classification.ROLE)
        .build();
  }

  private static List<MspPrincipal.MSPPrincipal> getRolesFor(String policyFor, String orgMSPId) {
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
}
