package hlf.java.rest.client.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import hlf.java.rest.client.model.BlockEventWriteSet;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;

@Slf4j
public class FabricEventParseUtil {

  static ObjectMapper mapper = new ObjectMapper();

  public static String getWriteInfoFromBlock(BlockEvent.TransactionEvent transactionEvent)
      throws JsonProcessingException, InvalidProtocolBufferException {
    List<BlockEventWriteSet> writes =
        getBlockEventWriteSet(transactionEvent.getTransactionActionInfos());
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
}
