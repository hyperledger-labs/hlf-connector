package hlf.java.rest.client.IT;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.model.ChannelOperationRequest;
import hlf.java.rest.client.model.MSPDTO;
import hlf.java.rest.client.model.Orderer;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import hlf.java.rest.client.service.ChannelService;
import hlf.java.rest.client.service.HFClientWrapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.LifecycleChaincodePackage;
import org.hyperledger.fabric.sdk.LifecycleInstallChaincodeProposalResponse;
import org.hyperledger.fabric.sdk.LifecycleInstallChaincodeRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmartContractIT {

  @Autowired ChaincodeOperationsService chaincodeOperationsService;
  @Autowired private HFClientWrapper hfClientWrapper;
  @Autowired ChannelService channelService;
  private static final String CHANNEL_NAME = "sc-test";
  private static final String CHAINCODE_NAME = "example-cc_1";
  private static final String CHAINCODE_VERSION = "1";

  @Test
  @Order(1)
  public void installChaincodeTest()
      throws InvalidArgumentException, IOException, ProposalException {

    createAndJoinChannel();
    Channel channel = hfClientWrapper.getHfClient().getChannel(CHANNEL_NAME);

    LifecycleChaincodePackage lifecycleChaincodePackage =
        LifecycleChaincodePackage.fromSource(
            CHAINCODE_NAME,
            Paths.get("src/test/java/hlf/java/rest/client/chaincode/"),
            TransactionRequest.Type.JAVA,
            "",
            null);
    String chaincodePackageID =
        lifecycleInstallChaincode(
            hfClientWrapper.getHfClient(), channel.getPeers(), lifecycleChaincodePackage);
    assertNotNull(chaincodePackageID);
  }

  private void createAndJoinChannel() {
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

      hlf.java.rest.client.model.Peer peer = new hlf.java.rest.client.model.Peer();
      peer.setName("peer1");
      peer.setMspid("Org1MSP");
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

      List<hlf.java.rest.client.model.Peer> peerList = new ArrayList<>();
      peerList.add(peer);
      channelOperationRequest.setPeers(peerList);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    channelService.createChannel(channelOperationRequest);
    channelService.joinChannel(channelOperationRequest);
  }

  private String lifecycleInstallChaincode(
      HFClient client, Collection<Peer> peers, LifecycleChaincodePackage lifecycleChaincodePackage)
      throws InvalidArgumentException, ProposalException {

    LifecycleInstallChaincodeRequest installProposalRequest =
        client.newLifecycleInstallChaincodeRequest();
    installProposalRequest.setLifecycleChaincodePackage(lifecycleChaincodePackage);
    installProposalRequest.setProposalWaitTime(1000000);

    Collection<LifecycleInstallChaincodeProposalResponse> responses =
        client.sendLifecycleInstallChaincodeRequest(installProposalRequest, peers);

    Collection<ProposalResponse> successful = new LinkedList<>();
    Collection<ProposalResponse> failed = new LinkedList<>();
    String packageID = null;
    for (LifecycleInstallChaincodeProposalResponse response : responses) {
      if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
        successful.add(response);
        if (packageID == null) {
          packageID = response.getPackageId();
          assertNotNull(
              format("Hashcode came back as null from peer: %s ", response.getPeer()), packageID);
        } else {
          assertEquals(
              "Miss match on what the peers returned back as the packageID",
              packageID,
              response.getPackageId());
        }
      } else {
        failed.add(response);
      }
    }

    if (failed.size() > 0) {
      ProposalResponse first = failed.iterator().next();
      fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
    }

    assertNotNull(packageID);
    assertFalse(packageID.isEmpty());

    return packageID;
  }

  @Test
  @Order(2)
  public void getCurrentSequenceTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    assertNotNull(sequence);
  }

  @Test
  @Order(3)
  public void getCurrentPackageIdTest() {

    // chaincode version changed to satisfy the new condition for fetching label
    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME,
            CHAINCODE_NAME.substring(0, CHAINCODE_NAME.length() - 2),
            CHAINCODE_VERSION);
    assertNotNull(packageId);
  }

  @Test
  @Order(4)
  public void approveChaincodeTest() throws InvalidArgumentException, ProposalException {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME,
            CHAINCODE_NAME.substring(0, CHAINCODE_NAME.length() - 2),
            CHAINCODE_VERSION);

    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(CHAINCODE_NAME)
            .chaincodePackageID(packageId)
            .chaincodeVersion(CHAINCODE_VERSION)
            .initRequired(false)
            .sequence(Long.parseLong(sequence))
            .build();
    String result =
        chaincodeOperationsService.performChaincodeOperation(
            CHANNEL_NAME, chaincodeOperations, ChaincodeOperationsType.approve, Optional.empty());
    assertNotNull(result);
  }

  @Test
  @Order(6)
  public void commitChaincodeTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME,
            CHAINCODE_NAME.substring(0, CHAINCODE_NAME.length() - 2),
            CHAINCODE_VERSION);

    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(CHAINCODE_NAME)
            .chaincodePackageID(packageId)
            .chaincodeVersion(CHAINCODE_VERSION)
            .initRequired(false)
            .sequence(Long.parseLong(sequence))
            .build();
    String result =
        chaincodeOperationsService.performChaincodeOperation(
            CHANNEL_NAME, chaincodeOperations, ChaincodeOperationsType.commit, Optional.empty());
    assertNotNull(result);
  }

  @Test
  @Order(5)
  public void getApprovedOrganizationsTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);

    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(CHAINCODE_NAME)
            .chaincodeVersion(CHAINCODE_VERSION)
            .sequence(Long.parseLong(sequence))
            .initRequired(false)
            .build();
    Set<String> organizationSet =
        chaincodeOperationsService.getApprovedOrganizations(
            CHANNEL_NAME, chaincodeOperations, Optional.empty(), Optional.empty());
    assertEquals(1, organizationSet.size());
    assertEquals("Org1MSP", organizationSet.iterator().next());
  }
}
