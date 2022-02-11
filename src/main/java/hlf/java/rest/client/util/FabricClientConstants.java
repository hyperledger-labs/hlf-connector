package hlf.java.rest.client.util;

public class FabricClientConstants {

  public static final String CHANNEL_NAME = "channel_name";
  public static final String FUNCTION_NAME = "function_name";
  public static final String CHAINCODE_NAME = "chaincode_name";
  public static final String ERROR_MSG = "error_msg";
  public static final String PEER_LIST = "peers";

  public static final String FABRIC_TRANSACTION_ID = "fabric_tx_id";
  public static final String FABRIC_EVENT_NAME = "event_name";
  public static final String FABRIC_CHANNEL_NAME = "channel_name";
  public static final String FABRIC_CHAINCODE_NAME = "chaincode_name";
  public static final String FABRIC_EVENT_TYPE = "event_type";
  public static final String FABRIC_EVENT_TYPE_CHAINCODE = "chaincode_event";
  public static final String FABRIC_EVENT_TYPE_BLOCK = "block_event";
  public static final String FABRIC_EVENT_TYPE_ERROR = "error_event";
  public static final String FABRIC_EVENT_FUNC_NAME = "function_name";
  public static final String FABRIC_TRANSIENT_KEY = "transient_key";
  public static final String FABRIC_COLLECTION_NAME = "collection_name";

  public static final String URI_PATH_BLOCKS = "/blocks/";
  public static final String URI_PATH_TRANSACTIONS = "/transactions/";
  public static final String URI_QUERY_PARAM_EVENT_TYPE = "eventType";
  public static final String URI_QUERY_PARAM_EVENTS = "/events";

  public static final String KAFKA_SECURITY_PROTOCOL_KEY = "security.protocol";
  public static final String KAFKA_SECURITY_PROTOCOL_VALUE = "SASL_SSL";
  public static final String KAFKA_SASL_MECHANISM_KEY = "sasl.mechanism";
  public static final String KAFKA_SASL_MECHANISM_VALUE = "PLAIN";
  public static final String KAFKA_SASL_JASS_ENDPOINT_KEY = "sasl.jaas.config";
  public static final Integer KAFKA_INTG_MAX_POLL_INTERVAL = 500000;
  public static final Integer KAFKA_INTG_MAX_POLL_RECORDS = 100;
  public static final Integer KAFKA_INTG_SESSION_TIMEOUT = 30000;
}
