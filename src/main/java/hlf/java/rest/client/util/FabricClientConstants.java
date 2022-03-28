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
  public static final String IS_PRIVATE_DATA_PRESENT = "is_private_data_present";
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
  
  public static final String CHANNEL_CONFIG_GROUP_APPLICATION = "Application";
  public static final String CHANNEL_CONFIG_MOD_POLICY_ADMINS = "Admins";
  
  public static final String CHANNEL_CONFIG_POLICY_TYPE_WRITERS = "Writers";
  public static final String CHANNEL_CONFIG_POLICY_TYPE_ENDORSEMENT = "Endorsement";
  public static final String CHANNEL_CONFIG_POLICY_TYPE_LIFECYCLE_ENDORSEMENT = "LifecycleEndorsement";
  public static final String CHANNEL_CONFIG_POLICY_TYPE_READERS = "Readers";
  public static final String CHANNEL_CONFIG_POLICY_TYPE_ADMINS = "Admins";
  
  public static final String CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ADMIN = "admin";
  public static final String CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_CLIENT = "client";
  public static final String CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_ORDERER = "orderer";
  public static final String CHANNEL_CONFIG_ORGANIZATIONAL_UNIT_ID_PEER = "peer";
  
  public static final String CHANNEL_CONFIG_GROUP_VALUE_MSP = "MSP";
  public static final String CHANNEL_CONFIG_GROUP_VALUE_ANCHORPEERS= "AnchorPeers";


  public static final String CHANNEL_CONFIG_IDENTITY_IDENTIFIER_SHA256 = "SHA256";
  public static final String CHANNEL_CONFIG_SIGNATURE_HASH_FAMILY_SHA2 = "SHA2";

  public static final String JSON_PATH_ANCHORPEERS_VALUE = "$..AnchorPeers.value";
  public static final String JSON_PATH_MSP_VALUE = "$..MSP.value";
  public static final String JSON_PATH_POLICY_TYPE_ONE_VALUE = "$..['policy'][?(@['type'] == " + 1 + ")]";
  public static final String JSON_PATH_POLICY_TYPE_THREE_VALUE = "$..['policy'][?(@['type'] == " + 3 + ")]";
  public static final String JSON_PATH_VALUE = "value";
  public static final String JSON_PATH_CAPABILITIES_VALUE = "$..Capabilities.value";
  public static final String JSON_PATH_ENDPOINTS_VALUE = "$..Endpoints.value";
  public static final String JSON_PATH_CONSENSUS_VALUE = "$..ConsensusType.value";
  public static final String JSON_PATH_BATCHSIZE_VALUE = "$..BatchSize.value";
  public static final String JSON_PATH_BATCHTIMEOUT_VALUE = "$..BatchTimeout.value";
  public static final String JSON_PATH_CONSORTIUM_VALUE ="$..Consortium.value";
  public static final String JSON_PATH_HASHINGALGORITHM_VALUE = "$..HashingAlgorithm.value";
  public static final String JSON_PATH_BLOCKHASHINGALGORITHM_VALUE = "$..BlockDataHashingStructure.value";
  public static final String JSON_PATH_METADATA_VALUE = "$..metadata";
  public static final String JSON_PATH_PRINCIPAL_VALUE = "$..principal";
  public static final String JSON_PATH_MSP_VALUE_CONFIG = "$..MSP.value.config";
  public static final String JSON_PATH_ROOTCERTS = "$..rootCerts";
  public static final String JSON_PATH_TLS_ROOT_CERTS = "$..tls_root_certs";
  public static final String JSON_PATH_CERTIFICATE = "$..certificate";

  
}