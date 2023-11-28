package hlf.java.rest.client.model;

import java.util.List;
import lombok.Data;

@Data
public class NewOrgParamsDTO {

  private String organizationName;
  private String organizationMspId;
  private MSPDTO mspDTO;
  private List<AnchorPeerDTO> anchorPeerDTOs;
}
