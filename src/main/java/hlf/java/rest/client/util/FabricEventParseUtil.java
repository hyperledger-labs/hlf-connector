package hlf.java.rest.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.model.BlockEventPrivateDataWriteSet;
import hlf.java.rest.client.model.BlockEventWriteSet;
import hlf.java.rest.client.model.EventStructure;
import hlf.java.rest.client.model.EventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.protos.ledger.rwset.Rwset;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.EventsPackage;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class FabricEventParseUtil {

  static ObjectMapper mapper = new ObjectMapper();

  public static String getWriteInfoFromBlock(BlockEvent.TransactionEvent transactionEvent)
      throws JsonProcessingException, InvalidProtocolBufferException {
    List<BlockEventWriteSet> writes =
        getBlockEventWriteSet(transactionEvent.getTransactionActionInfos());
    return mapper.writeValueAsString(writes);
  }

  public static String getPrivateDataFromBlock(
      EventsPackage.BlockAndPrivateData blockAndPrivateData)
      throws InvalidProtocolBufferException, JsonProcessingException {
    if (blockAndPrivateData == null) {
      return "";
    }
    List<BlockEventPrivateDataWriteSet> writes =
        getPrivateDataBlockEventWriteSet(blockAndPrivateData.getPrivateDataMapMap());
    return mapper.writeValueAsString(writes);
  }

  public static List<BlockEventWriteSet> getBlockEventWriteSet(
      Iterable<BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo> transactionActionInfos)
      throws InvalidProtocolBufferException {

    List<KvRwset.KVWrite> writeList = new ArrayList<>();
    for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo txnActionInfo :
        transactionActionInfos) {
      Iterable<TxReadWriteSetInfo.NsRwsetInfo> rwSetInfos =
          txnActionInfo.getTxReadWriteSet().getNsRwsetInfos();
      for (TxReadWriteSetInfo.NsRwsetInfo rwSetInfo : rwSetInfos) {
        writeList.addAll(rwSetInfo.getRwset().getWritesList());
      }
    }

    List<BlockEventWriteSet> writes = new ArrayList<>();
    for (KvRwset.KVWrite writeSet : writeList) {
      String key = cleanTextContent(writeSet.getKey());
      String value = cleanTextContent(writeSet.getValue().toStringUtf8());
      boolean isDelete = writeSet.getIsDelete();
      writes.add(getBlockEventWriteSet(key, value, isDelete));
    }

    return writes;
  }

  public static List<BlockEventPrivateDataWriteSet> getPrivateDataBlockEventWriteSet(
      Map<Long, Rwset.TxPvtReadWriteSet> privateDataMap) throws InvalidProtocolBufferException {
    List<BlockEventPrivateDataWriteSet> writes = new ArrayList<>();
    for (Map.Entry<Long, Rwset.TxPvtReadWriteSet> privateData : privateDataMap.entrySet()) {
      Rwset.TxPvtReadWriteSet privateDataValue = privateData.getValue();
      Rwset.TxReadWriteSet.DataModel dataModel = privateDataValue.getDataModel();
      List<Rwset.NsPvtReadWriteSet> privateDataNsList = privateDataValue.getNsPvtRwsetList();
      for (Rwset.NsPvtReadWriteSet nsRWSet : privateDataNsList) {
        String namespace = cleanTextContent(nsRWSet.getNamespace());
        List<Rwset.CollectionPvtReadWriteSet> collectionRwSet = nsRWSet.getCollectionPvtRwsetList();
        for (Rwset.CollectionPvtReadWriteSet pvtRWSet : collectionRwSet) {
          String collectionName = cleanTextContent(pvtRWSet.getCollectionName());
          ByteString serializedRWSet = pvtRWSet.getRwset();
          switch (dataModel) {
            case KV:
              KvRwset.KVRWSet kvrwset = KvRwset.KVRWSet.parseFrom(serializedRWSet);
              // for each KVRWSet add an entry to send back
              for (KvRwset.KVWrite writeSet : kvrwset.getWritesList()) {
                String key = cleanTextContent(writeSet.getKey());
                String value = cleanTextContent(writeSet.getValue().toStringUtf8());
                boolean isDelete = writeSet.getIsDelete();
                writes.add(
                    getBlockEventPrivateDataWriteSet(
                        namespace, collectionName, key, value, isDelete));
              }
              break;
            default:
              throw new ServiceException(
                  ErrorCode.HYPERLEDGER_FABRIC_NOT_SUPPORTED, "Private Data but not KV Set");
          }
        }
      }
    }
    return writes;
  }

  public static List<String> getChaincodeEventWriteSet(
      Iterable<BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo> transactionActionInfos) {

    List<String> chaincodeEvents = new ArrayList<>();
    for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo :
        transactionActionInfos) {
      chaincodeEvents.add(new String(transactionActionInfo.getEvent().getPayload()));
    }
    return chaincodeEvents;
  }

  private static BlockEventWriteSet getBlockEventWriteSet(
      String key, String value, boolean isDelete) {
    BlockEventWriteSet blockEventWriteSet = new BlockEventWriteSet();
    blockEventWriteSet.setKey(key);
    blockEventWriteSet.setValue(value);
    blockEventWriteSet.setDelete(isDelete);
    return blockEventWriteSet;
  }

  private static BlockEventPrivateDataWriteSet getBlockEventPrivateDataWriteSet(
      String namespace, String collectionName, String key, String value, boolean isDelete) {
    BlockEventPrivateDataWriteSet blockEventWriteSet = new BlockEventPrivateDataWriteSet();
    blockEventWriteSet.setCollectionName(collectionName);
    blockEventWriteSet.setNamespace(namespace);
    blockEventWriteSet.setKey(key);
    blockEventWriteSet.setValue(value);
    blockEventWriteSet.setDelete(isDelete);
    return blockEventWriteSet;
  }

  private static String cleanTextContent(String text) {
    // strips off all non-ASCII characters
    text = text.replaceAll("[^\\x00-\\x7F]", "");
    // erases all the ASCII control characters
    text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    // removes non-printable characters from Unicode
    text = text.replaceAll("\\p{C}", "");
    return text.trim();
  }

  public static String stringify(Object obj) throws JsonProcessingException {
    return mapper.writeValueAsString(obj);
  }

  public static String createEventStructure(
      String data,
      String privateDataPayload,
      String transactionId,
      Long blockNumber,
      EventType eventType) {
    String uri =
        UriComponentsBuilder.fromUriString(
                FabricClientConstants.URI_PATH_BLOCKS
                    + blockNumber
                    + FabricClientConstants.URI_PATH_TRANSACTIONS
                    + transactionId
                    + FabricClientConstants.URI_QUERY_PARAM_EVENTS)
            .queryParam(FabricClientConstants.URI_QUERY_PARAM_EVENT_TYPE, eventType.toString())
            .build()
            .toUriString();
    String message = null;
    try {
      message =
          FabricEventParseUtil.stringify(
              EventStructure.builder()
                  .data(data)
                  .privateData(privateDataPayload)
                  .eventURI(uri)
                  .build());
    } catch (JsonProcessingException e) {
      log.error("Error in transforming the event into Json format for Kafka ", e);
    }
    return message;
  }
}
