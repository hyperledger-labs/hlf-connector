package hlf.java.rest.client.IT;

import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.ClientResponseModel;
import hlf.java.rest.client.model.MSPDTO;
import hlf.java.rest.client.model.NewOrgParamsDTO;
import hlf.java.rest.client.model.Orderer;
import hlf.java.rest.client.model.Peer;
import hlf.java.rest.client.service.ChannelService;
import hlf.java.rest.client.service.NetworkStatus;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChannelIT {

  @Autowired ChannelService channelService;
  @Autowired NetworkStatus networkStatus;
  private static final String CHANNEL_NAME = "test1";
  private static final String ORG_1_MSP = "Org1MSP";
  private static final String CHANNEL_NAME_TWO_ORGS = "test2";
  private static final String ORG_2_MSP = "Org2MSP";

  @Test
  @Order(1)
  public void createChannelTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName(CHANNEL_NAME);
    channelOperationRequest.setConsortiumName("SampleConsortium");
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
                  .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer = new Peer();
      peer.setName("peer1");
      peer.setMspid(ORG_1_MSP);
      peer.setGrpcUrl("grpc://localhost:7051");
      String admincert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
                  .toFile());
      String cacert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/cacerts/ca.org1.example.com-cert.pem")
                  .toFile());
      peer.setCertificate(cacert);

      MSPDTO mspdto = new MSPDTO();
      List<String> rootCerts = new ArrayList<>();
      rootCerts.add(cacert);
      mspdto.setRootCerts(rootCerts);
      List<String> tlsRootCerts = new ArrayList<>();
      tlsRootCerts.add(cacert);
      mspdto.setTlsRootCerts(tlsRootCerts);
      mspdto.setAdminOUCert(admincert);
      mspdto.setClientOUCert(admincert);
      mspdto.setPeerOUCert(cacert);
      peer.setMspDTO(mspdto);

      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer);
      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.createChannel(channelOperationRequest);
    Assertions.assertEquals(new Integer(200), clientResponseModel.getCode());
  }

  @Test
  @Order(2)
  public void joinChannelTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName(CHANNEL_NAME);
    channelOperationRequest.setConsortiumName("SampleConsortium");
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
                  .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer = new Peer();
      peer.setName("peer1");
      peer.setMspid(ORG_1_MSP);
      peer.setGrpcUrl("grpc://localhost:7051");
      String admincert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
                  .toFile());
      String cacert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/cacerts/ca.org1.example.com-cert.pem")
                  .toFile());
      peer.setCertificate(cacert);

      MSPDTO mspdto = new MSPDTO();
      List<String> rootCerts = new ArrayList<>();
      rootCerts.add(cacert);
      mspdto.setRootCerts(rootCerts);
      List<String> tlsRootCerts = new ArrayList<>();
      tlsRootCerts.add(cacert);
      mspdto.setTlsRootCerts(tlsRootCerts);
      mspdto.setAdminOUCert(admincert);
      mspdto.setClientOUCert(admincert);
      mspdto.setPeerOUCert(cacert);
      peer.setMspDTO(mspdto);

      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer);
      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.joinChannel(channelOperationRequest);
    Assertions.assertEquals(new Integer(200), clientResponseModel.getCode());
  }

  @Test
  @Order(3)
  public void getChannelMembersMSPIDTest() {
    Set<String> mspidSet = channelService.getChannelMembersMSPID(CHANNEL_NAME);
    Assertions.assertEquals(ORG_1_MSP, mspidSet.iterator().next());
  }

  @Test
  @Order(4)
  public void channelCreationForTwoOrgsTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName(CHANNEL_NAME_TWO_ORGS);
    channelOperationRequest.setConsortiumName("SampleConsortium");
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
                  .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer0Org1 = new Peer();
      peer0Org1.setName("peer1");
      peer0Org1.setMspid(ORG_1_MSP);
      peer0Org1.setGrpcUrl("grpc://localhost:7051");
      String admincert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
                  .toFile());
      String cacert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/cacerts/ca.org1.example.com-cert.pem")
                  .toFile());
      peer0Org1.setCertificate(cacert);

      MSPDTO mspdto = new MSPDTO();
      List<String> rootCerts = new ArrayList<>();
      rootCerts.add(cacert);
      mspdto.setRootCerts(rootCerts);
      List<String> tlsRootCerts = new ArrayList<>();
      tlsRootCerts.add(cacert);
      mspdto.setTlsRootCerts(tlsRootCerts);
      mspdto.setAdminOUCert(admincert);
      mspdto.setClientOUCert(admincert);
      mspdto.setPeerOUCert(cacert);
      peer0Org1.setMspDTO(mspdto);

      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer0Org1);

      Peer peer1Org2 = new Peer();
      peer1Org2.setName("peer2");
      peer1Org2.setMspid(ORG_2_MSP);
      peer1Org2.setGrpcUrl("grpc://localhost:8056");
      String admincert2 =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/msp/admincerts/Admin@org2.example.com-cert.pem")
                  .toFile());
      String cacert2 =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/msp/cacerts/ca.org2.example.com-cert.pem")
                  .toFile());
      peer1Org2.setCertificate(cacert2);

      MSPDTO mspdto2 = new MSPDTO();
      List<String> rootCerts2 = new ArrayList<>();
      rootCerts2.add(cacert2);
      mspdto2.setRootCerts(rootCerts2);
      List<String> tlsRootCerts2 = new ArrayList<>();
      tlsRootCerts2.add(cacert2);
      mspdto2.setTlsRootCerts(tlsRootCerts2);
      mspdto2.setAdminOUCert(admincert2);
      mspdto2.setClientOUCert(admincert2);
      mspdto2.setPeerOUCert(cacert2);
      peer1Org2.setMspDTO(mspdto2);

      peerList.add(peer1Org2);

      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.createChannel(channelOperationRequest);
    Assertions.assertEquals(new Integer(200), clientResponseModel.getCode());
  }

  @Test
  @Order(5)
  public void channelJoinForTwoOrgsTest() {
    ChannelOperationRequest channelOperationRequest = new ChannelOperationRequest();
    channelOperationRequest.setChannelName(CHANNEL_NAME_TWO_ORGS);
    channelOperationRequest.setConsortiumName("SampleConsortium");
    try {
      Orderer orderer = new Orderer();
      orderer.setName("orderer1");
      orderer.setGrpcUrl("grpc://localhost:7050");
      orderer.setCertificate(
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/ordererOrganizations/example.com/msp/admincerts/Admin@example.com-cert.pem")
                  .toFile()));
      List<Orderer> ordererList = new ArrayList<>();
      ordererList.add(orderer);
      channelOperationRequest.setOrderers(ordererList);

      Peer peer0Org1 = new Peer();
      peer0Org1.setName("peer1");
      peer0Org1.setMspid(ORG_1_MSP);
      peer0Org1.setGrpcUrl("grpc://localhost:7051");
      String admincert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/admincerts/Admin@org1.example.com-cert.pem")
                  .toFile());
      String cacert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp/cacerts/ca.org1.example.com-cert.pem")
                  .toFile());
      peer0Org1.setCertificate(cacert);

      MSPDTO mspdto = new MSPDTO();
      List<String> rootCerts = new ArrayList<>();
      rootCerts.add(cacert);
      mspdto.setRootCerts(rootCerts);
      List<String> tlsRootCerts = new ArrayList<>();
      tlsRootCerts.add(cacert);
      mspdto.setTlsRootCerts(tlsRootCerts);
      mspdto.setAdminOUCert(admincert);
      mspdto.setClientOUCert(admincert);
      mspdto.setPeerOUCert(cacert);
      peer0Org1.setMspDTO(mspdto);

      List<Peer> peerList = new ArrayList<>();
      peerList.add(peer0Org1);

      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ClientResponseModel clientResponseModel = channelService.joinChannel(channelOperationRequest);
    Assertions.assertEquals(new Integer(200), clientResponseModel.getCode());
  }

  @Test
  @Order(6)
  public void addOrgToChannelTest() {
    NewOrgParamsDTO newOrgParamsDTO = new NewOrgParamsDTO();
    newOrgParamsDTO.setOrganizationName(ORG_2_MSP);
    try {
      String signcert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/msp/signcerts/peer1.org2.example.com-cert.pem")
                  .toFile());
      String cacert =
          FileUtils.readFileToString(
              Paths.get(
                      "src/test/java/fabricSetup/e2e-2Orgs/v2.1/crypto-config/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/msp/cacerts/ca.org2.example.com-cert.pem")
                  .toFile());

      MSPDTO mspdto = new MSPDTO();
      List<String> rootCerts = new ArrayList<>();
      rootCerts.add(cacert);
      mspdto.setRootCerts(rootCerts);
      List<String> tlsRootCerts = new ArrayList<>();
      tlsRootCerts.add(cacert);
      mspdto.setTlsRootCerts(tlsRootCerts);
      mspdto.setAdminOUCert(cacert);
      mspdto.setClientOUCert(cacert);
      mspdto.setPeerOUCert(cacert);

      newOrgParamsDTO.setMspDTO(mspdto);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ResponseEntity<ClientResponseModel> responseModel =
        networkStatus.addOrgToChannel(CHANNEL_NAME, newOrgParamsDTO);
    Assertions.assertEquals(new Integer(200), responseModel.getStatusCodeValue());
  }
}
