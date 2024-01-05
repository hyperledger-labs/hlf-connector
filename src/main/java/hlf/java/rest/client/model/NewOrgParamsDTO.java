package hlf.java.rest.client.model;

import lombok.Data;

@Data
public class NewOrgParamsDTO extends ChannelUpdateParamsDTO {
  private String organizationName;
  private MSPDTO mspDTO;
}
