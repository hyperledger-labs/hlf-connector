package hlf.java.rest.client.IT;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import hlf.java.rest.client.model.ChaincodeOperations;
import hlf.java.rest.client.model.ChaincodeOperationsType;
import hlf.java.rest.client.service.ChaincodeOperationsService;
import hlf.java.rest.client.service.HFClientWrapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import org.hyperledger.fabric.gateway.Gateway;
import org.hyperledger.fabric.gateway.Network;
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
  @Autowired private Gateway gateway;
  private static final String CHANNEL_NAME = "test1";
  private static final String CHAINCODE_NAME = "example-cc-1";
  private static final String CHAINCODE_VERSION = "1";

  @Test
  @Order(1)
  public void installChaincodeTest()
      throws InvalidArgumentException, IOException, ProposalException {

    Network network = gateway.getNetwork(CHANNEL_NAME);
    Channel channel = network.getChannel();

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

    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    assertNotNull(packageId);
  }

  @Test
  @Order(4)
  public void approveChaincodeTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);

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
            CHANNEL_NAME, chaincodeOperations, ChaincodeOperationsType.approve);
    assertNotNull(result);
  }

  @Test
  @Order(5)
  public void commitChaincodeTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);
    String packageId =
        chaincodeOperationsService.getCurrentPackageId(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);

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
            CHANNEL_NAME, chaincodeOperations, ChaincodeOperationsType.commit);
    assertNotNull(result);
  }

  @Test
  @Order(6)
  public void getApprovedOrganizationsTest() {

    String sequence =
        chaincodeOperationsService.getCurrentSequence(
            CHANNEL_NAME, CHAINCODE_NAME, CHAINCODE_VERSION);

    ChaincodeOperations chaincodeOperations =
        ChaincodeOperations.builder()
            .chaincodeName(CHAINCODE_NAME)
            .chaincodeVersion(CHAINCODE_VERSION)
            .sequence(Long.parseLong(sequence) + 1)
            .initRequired(false)
            .build();
    Set<String> organizationSet =
        chaincodeOperationsService.getApprovedOrganizations(
            CHANNEL_NAME, chaincodeOperations, Optional.empty(), Optional.empty());
    assertEquals(1, organizationSet.size());
    assertEquals("Org1MSP", organizationSet.iterator().next());
  }
}
