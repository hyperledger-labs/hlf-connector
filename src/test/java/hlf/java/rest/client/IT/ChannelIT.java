package hlf.java.rest.client.IT;

import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.Orderer;
import hlf.java.rest.client.model.Peer;
import hlf.java.rest.client.service.ChannelService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChannelIT {

  @Autowired ChannelService channelService;

  @Test
  @Order(1)
  public void createChannelTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName("testChannel");
    channelOperationRequest.setConsortiumName("SampleConsortium");
    List<String> mspList = new ArrayList<>();
    mspList.add("Org1MSP");
    channelOperationRequest.setOrgMsps(mspList);
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(FileUtils.readFileToString(Paths.get(
              "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
          .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer = new Peer();
      peer.setName("peer1");
      peer.setGrpcUrl("grpc://localhost:7051");
      peer.setCertificate(FileUtils.readFileToString(Paths.get(
              "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
          .toFile()));
      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer);
      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.createChannel(channelOperationRequest);
    Assertions.assertEquals(clientResponseModel.getCode(), new Integer(200));
  }

  @Test
  @Order(2)
  public void joinChannelTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName("testChannel");
    channelOperationRequest.setConsortiumName("SampleConsortium");
    List<String> mspList = new ArrayList<>();
    mspList.add("Org1MSP");
    channelOperationRequest.setOrgMsps(mspList);
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(FileUtils.readFileToString(Paths.get(
              "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
          .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer = new Peer();
      peer.setName("peer1");
      peer.setGrpcUrl("grpc://localhost:7051");
      peer.setCertificate(FileUtils.readFileToString(Paths.get(
              "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
          .toFile()));
      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer);
      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.joinChannel(channelOperationRequest);
    Assertions.assertEquals(clientResponseModel.getCode(), new Integer(200));
  }

  @Test
  @Order(3)
  public void getChannelMembersMSPIDTest() {
    Set<String> mspidSet = channelService.getChannelMembersMSPID("testChannel");
    Assertions.assertEquals("Org1MSP", mspidSet.iterator().next());
  }

  @Test
  public void getChannelMembersMSPIDTestForNonExistingChannel(){
    Set<String> mspidSet = channelService.getChannelMembersMSPID("dummyChannel");
    //channel will get created based on details provided in connection.yml
    Assertions.assertEquals("Org1MSP", mspidSet.iterator().next());
  }
}
