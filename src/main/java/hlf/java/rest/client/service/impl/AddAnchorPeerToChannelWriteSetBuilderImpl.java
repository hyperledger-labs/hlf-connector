package hlf.java.rest.client.service.impl;

import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AnchorPeerDTO;
import hlf.java.rest.client.model.AnchorPeerParamsDTO;
import hlf.java.rest.client.service.AddAnchorPeerToChannelWriteSetBuilder;
import hlf.java.rest.client.util.FabricChannelUtil;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.common.Configtx.ConfigValue;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeer;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeers;
import org.springframework.stereotype.Service;

@Service
public class AddAnchorPeerToChannelWriteSetBuilderImpl
    implements AddAnchorPeerToChannelWriteSetBuilder {

  private AnchorPeerParamsDTO anchorPeerParamsDTO;

  @Override
  public ConfigGroup buildWriteSetForAnchorPeers(
      ConfigGroup readset, AnchorPeerParamsDTO anchorPeerParamsDTO) throws ServiceException {
    this.anchorPeerParamsDTO = anchorPeerParamsDTO;
    String orgMspId = anchorPeerParamsDTO.getOrganizationMspId();
    Map<String, ConfigGroup> existingOrganizations =
        FabricChannelUtil.getExistingOrgsFromReadset(readset);
    // The "Application" group
    ConfigGroup applicationGroup =
        ConfigGroup.newBuilder()
            .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
            .putAllPolicies(FabricChannelUtil.setApplicationPolicies(readset))
            .putGroups(orgMspId, setAnchorPeerInGroup(orgMspId, readset))
            // putAllGroups excludes new organization
            .putAllGroups(existingOrganizations)
            // Application group version
            .setVersion(
                FabricChannelUtil.retrieveMSPGroupVersionFromReadset(
                        readset, FabricClientConstants.CHANNEL_CONFIG_GROUP_APPLICATION)
                    + 1) //  will be tied to current version + 1 for this level
            .build();
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
    if (anchorPeerParamsDTO.getAnchorPeerDTOs() != null) {
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

  private ConfigValue setNewOrgAnchorPeerValue() {
    return ConfigValue.newBuilder()
        .setModPolicy(FabricClientConstants.CHANNEL_CONFIG_MOD_POLICY_ADMINS)
        .setValue(setAnchorPeers().toByteString())
        .setVersion(0)
        .build();
  }

  private AnchorPeers setAnchorPeers() {
    List<AnchorPeer> anchorPeerList = new ArrayList<>();
    for (AnchorPeerDTO anchorPeerDTO : anchorPeerParamsDTO.getAnchorPeerDTOs()) {
      anchorPeerList.add(
          AnchorPeer.newBuilder()
              .setHost(anchorPeerDTO.getHostname())
              .setPort(anchorPeerDTO.getPort())
              .build());
    }
    return AnchorPeers.newBuilder().addAllAnchorPeers(anchorPeerList).build();
  }
}
