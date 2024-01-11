package hlf.java.rest.client.model;

import java.util.List;
import lombok.Data;

@Data
public class ChannelUpdateParamsDTO {
  private String organizationMspId;
  private List<AnchorPeerDTO> anchorPeerDTOs;
  private MSPDTO mspDTO;
}
