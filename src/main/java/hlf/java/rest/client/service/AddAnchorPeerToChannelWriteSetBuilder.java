package hlf.java.rest.client.service;

import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.AnchorPeerParamsDTO;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;

public interface AddAnchorPeerToChannelWriteSetBuilder {

  ConfigGroup buildWriteSetForAnchorPeers(
      ConfigGroup readset, AnchorPeerParamsDTO anchorPeerParamsDTO) throws ServiceException;
}
