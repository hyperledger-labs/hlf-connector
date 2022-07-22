package hlf.java.rest.client.service;

import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;

import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.NewOrgParamsDTO;

public interface AddOrgToChannelWriteSetBuilder {

  ConfigGroup buildWriteset(ConfigGroup readset, NewOrgParamsDTO organizationDetails)
      throws ServiceException;

}
