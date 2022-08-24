package hlf.java.rest.client.service;

import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;

public interface AddOrgToChannelWriteSetBuilder {

  ConfigGroup buildWriteset(ConfigGroup readset, NewOrgParamsDTO organizationDetails)
      throws ServiceException;
}
