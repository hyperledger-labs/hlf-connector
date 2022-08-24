package hlf.java.rest.client.service;

import hlf.java.rest.client.exception.ServiceException;

public interface ChannelConfigDeserialization {

  String deserializeValueFields(String channelConfigString) throws ServiceException;
}
