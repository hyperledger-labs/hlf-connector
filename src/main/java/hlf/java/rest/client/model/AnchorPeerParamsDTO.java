package hlf.java.rest.client.model;

import java.util.List;
import lombok.Data;

@Data
public class AnchorPeerParamsDTO {
  private String organizationMspId;
  private List<AnchorPeerDTO> anchorPeerDTOs;
}
