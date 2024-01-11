package hlf.java.rest.client.service;

import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.ChannelUpdateParamsDTO;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;

public interface UpdateChannel {

  ConfigGroup buildWriteset(ConfigGroup readset, ChannelUpdateParamsDTO organizationDetails)
      throws ServiceException;
}
