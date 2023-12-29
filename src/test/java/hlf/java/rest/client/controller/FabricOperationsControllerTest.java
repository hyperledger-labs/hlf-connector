package hlf.java.rest.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hlf.java.rest.client.model.AnchorPeerParamsDTO;
import hlf.java.rest.client.model.CommitChannelParamsDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.service.NetworkStatus;
import hlf.java.rest.client.util.SerializationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@ExtendWith(MockitoExtension.class)
@DirtiesContext
public class FabricOperationsControllerTest {

  @InjectMocks private FabricOperationsController fabricOperationsController;
  @Mock private NetworkStatus networkStatus;
  @Mock private SerializationUtil serializationUtil;

  @Test
  public void getChannelConfigurationTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(networkStatus.getChannelFromNetwork(Mockito.anyString()))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity, fabricOperationsController.getChannelConfiguration("some_channel_name"));
  }

  @Test
  public void generateConfigUpdateFileTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            networkStatus.generateConfigUpdate(
                Mockito.anyString(), Mockito.any(NewOrgParamsDTO.class)))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity,
        fabricOperationsController.generateConfigUpdateFile(
            "some_channel_name", new NewOrgParamsDTO()));
  }

  @Test
  public void signChannelConfigTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            networkStatus.signChannelConfigTransaction(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity,
        fabricOperationsController.signChannelConfig("some_channel_name", "channel_update_file"));
  }

  @Test
  public void commitSignedChannelConfigTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            networkStatus.commitChannelConfigTransaction(
                Mockito.anyString(), Mockito.any(CommitChannelParamsDTO.class)))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity,
        fabricOperationsController.commitSignedChannelConfig(
            "some_channel_name", new CommitChannelParamsDTO()));
  }

  @Test
  public void commitAddOrgToChannelTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            networkStatus.addOrgToChannel(Mockito.anyString(), Mockito.any(NewOrgParamsDTO.class)))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity,
        fabricOperationsController.addOrgToChannel("some_channel_name", new NewOrgParamsDTO()));
  }

  @Test
  public void getDeserializedJsonTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            serializationUtil.decodeContents(
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity, serializationUtil.decodeContents("channel_update_file", true, true));
  }

  @Test
  public void addAnchorPeersToChannelTest() {
    ResponseEntity responseEntity = new ResponseEntity(HttpStatus.OK);
    Mockito.when(
            networkStatus.addAnchorPeersToChannel(
                Mockito.anyString(), Mockito.any(AnchorPeerParamsDTO.class)))
        .thenReturn(responseEntity);
    assertEquals(
        responseEntity,
        fabricOperationsController.addAnchorPeersToChannel(
            "some_channel_name", new AnchorPeerParamsDTO()));
  }
}
