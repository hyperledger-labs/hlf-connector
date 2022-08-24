package hlf.java.rest.client.service.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.jayway.jsonpath.JsonPath;
import hlf.java.rest.client.exception.ErrorCode;
import hlf.java.rest.client.exception.ServiceException;
import hlf.java.rest.client.service.ChannelConfigDeserialization;
import hlf.java.rest.client.util.FabricClientConstants;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.hyperledger.fabric.protos.common.Configuration.BlockDataHashingStructure;
import org.hyperledger.fabric.protos.common.Configuration.Capabilities;
import org.hyperledger.fabric.protos.common.Configuration.Consortium;
import org.hyperledger.fabric.protos.common.Configuration.HashingAlgorithm;
import org.hyperledger.fabric.protos.common.Configuration.OrdererAddresses;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPRole;
import org.hyperledger.fabric.protos.common.Policies.ImplicitMetaPolicy;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicyEnvelope;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.FabricMSPConfig;
import org.hyperledger.fabric.protos.msp.MspConfigPackage.MSPConfig;
import org.hyperledger.fabric.protos.orderer.Configuration.BatchSize;
import org.hyperledger.fabric.protos.orderer.Configuration.BatchTimeout;
import org.hyperledger.fabric.protos.orderer.Configuration.ConsensusType;
import org.hyperledger.fabric.protos.orderer.etcdraft.Metadata.BlockMetadata;
import org.hyperledger.fabric.protos.peer.Configuration.AnchorPeers;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ChannelConfigDeserializationImpl implements ChannelConfigDeserialization {

  @Override
  public String deserializeValueFields(String channelConfigString) throws ServiceException {
    try {
      channelConfigString = deserializeAnchorPeerValues(channelConfigString);
      channelConfigString = deserializeMspValues(channelConfigString);
      channelConfigString = deserializePolicyTypeOneValues(channelConfigString);
      channelConfigString = deserializePolicyTypeThreeValues(channelConfigString);
      channelConfigString = deserializeCapabilitiesValues(channelConfigString);
      channelConfigString = deserializeEndpointsValues(channelConfigString);
      channelConfigString = deserializeConsensusTypeValues(channelConfigString);
      channelConfigString = deserializeBatchSizeTypeValues(channelConfigString);
      channelConfigString = deserializeBatchTimeoutValues(channelConfigString);
      channelConfigString = deserializeConsortiumValues(channelConfigString);
      channelConfigString = deserializeHashingAlgorithmValues(channelConfigString);
      channelConfigString = deserializeBlockDataHashingStructureValues(channelConfigString);
      channelConfigString = deserializeMetadataValues(channelConfigString);
      channelConfigString = deserializePrincipalValues(channelConfigString);
      channelConfigString = deserializeMspValueConfigValues(channelConfigString);
      channelConfigString = deserializeRootCertsValues(channelConfigString);

    } catch (Exception e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the Channel Configuration file's base64 Values :"
              + e.getMessage(),
          e);
    }
    return channelConfigString;
  }

  private String deserializeAnchorPeerValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_ANCHORPEERS_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        AnchorPeers anchorPeers =
            AnchorPeers.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(anchorPeers);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the AnchorPeers values", e);
    }
    return channelConfigString;
  }

  private String deserializeMspValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_MSP_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        MSPConfig mspConfig =
            MSPConfig.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(mspConfig);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the MSP values", e);
    }
    return channelConfigString;
  }

  private String channelConfigStringForEachBypass;

  private String deserializePolicyTypeOneValues(String channelConfigString)
      throws ServiceException {
    List<LinkedHashMap> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_POLICY_TYPE_ONE_VALUE);
    channelConfigStringForEachBypass = channelConfigString;
    for (LinkedHashMap<?, ?> base64EncodedValueString : valueList) {
      base64EncodedValueString.forEach(
          (k, v) -> {
            try {
              signaturePolicyEnvelopeDeserialization(k, v);
            } catch (InvalidProtocolBufferException e) {
              throw new ServiceException(
                  ErrorCode.DESERIALIZATION_FAILURE,
                  "Error while deserializing the Policy Type 3 values",
                  e);
            }
          });
    }
    return channelConfigStringForEachBypass;
  }

  private void signaturePolicyEnvelopeDeserialization(Object key, Object value)
      throws InvalidProtocolBufferException {
    if (key.equals("value")) {
      SignaturePolicyEnvelope policy =
          SignaturePolicyEnvelope.parseFrom(Base64.getDecoder().decode((String) value));
      String base64DecodedString = JsonFormat.printer().print(policy);
      channelConfigStringForEachBypass =
          channelConfigStringForEachBypass.replace("\"" + value + "\"", base64DecodedString);
    }
  }

  private String deserializePolicyTypeThreeValues(String channelConfigString)
      throws ServiceException {
    List<LinkedHashMap> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_POLICY_TYPE_THREE_VALUE);
    channelConfigStringForEachBypass = channelConfigString;
    for (LinkedHashMap<?, ?> base64EncodedValueString : valueList) {
      base64EncodedValueString.forEach(
          (k, v) -> {
            try {
              implicitMetaPolicyDeserialization(k, v);
            } catch (InvalidProtocolBufferException e) {
              throw new ServiceException(
                  ErrorCode.DESERIALIZATION_FAILURE,
                  "Error while deserializing the Policy Type 3 values",
                  e);
            }
          });
    }
    return channelConfigStringForEachBypass;
  }

  private void implicitMetaPolicyDeserialization(Object key, Object value)
      throws InvalidProtocolBufferException {
    if (key.equals(FabricClientConstants.JSON_PATH_VALUE)) {
      ImplicitMetaPolicy policy =
          ImplicitMetaPolicy.parseFrom(Base64.getDecoder().decode((String) value));
      String base64DecodedString = JsonFormat.printer().print(policy);
      channelConfigStringForEachBypass =
          channelConfigStringForEachBypass.replace("\"" + value + "\"", base64DecodedString);
    }
  }

  private String deserializeCapabilitiesValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_CAPABILITIES_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        Capabilities capabilities =
            Capabilities.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(capabilities);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the Capabilities values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeEndpointsValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_ENDPOINTS_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        OrdererAddresses endpoints =
            OrdererAddresses.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(endpoints);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the Endpoints/OrdererAddresses values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeConsensusTypeValues(String channelConfigString)
      throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_CONSENSUS_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        ConsensusType consensusType =
            ConsensusType.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(consensusType);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the ConsensusType values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeBatchSizeTypeValues(String channelConfigString)
      throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_BATCHSIZE_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        BatchSize batchSize =
            BatchSize.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(batchSize);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the BatchSize values", e);
    }
    return channelConfigString;
  }

  private String deserializeBatchTimeoutValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_BATCHTIMEOUT_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        BatchTimeout batchTimeout =
            BatchTimeout.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(batchTimeout);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the BatchTimeout values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeConsortiumValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_CONSORTIUM_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        Consortium consortium =
            Consortium.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(consortium);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the Consortium values", e);
    }
    return channelConfigString;
  }

  private String deserializeHashingAlgorithmValues(String channelConfigString)
      throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_HASHINGALGORITHM_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        HashingAlgorithm hashingAlgorithm =
            HashingAlgorithm.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(hashingAlgorithm);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the HashingAlgorithm values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeBlockDataHashingStructureValues(String channelConfigString)
      throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString)
            .read(FabricClientConstants.JSON_PATH_BLOCKHASHINGALGORITHM_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        BlockDataHashingStructure blockDataHashingStructure =
            BlockDataHashingStructure.parseFrom(
                Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(blockDataHashingStructure);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the BlockDataHashingStructure values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeMetadataValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_METADATA_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        BlockMetadata metadata =
            BlockMetadata.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(metadata);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the Metadata values", e);
    }
    return channelConfigString;
  }

  private String deserializePrincipalValues(String channelConfigString) throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_PRINCIPAL_VALUE);
    try {
      for (String base64EncodedValueString : valueList) {
        MSPRole mspRole = MSPRole.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(mspRole);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE, "Error while deserializing the Principal values", e);
    }
    return channelConfigString;
  }

  private String deserializeMspValueConfigValues(String channelConfigString)
      throws ServiceException {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_MSP_VALUE_CONFIG);
    try {
      for (String base64EncodedValueString : valueList) {
        FabricMSPConfig mspConfig =
            FabricMSPConfig.parseFrom(Base64.getDecoder().decode(base64EncodedValueString));
        String base64DecodedString = JsonFormat.printer().print(mspConfig);
        channelConfigString =
            channelConfigString.replace(
                "\"" + base64EncodedValueString + "\"", base64DecodedString);
      }
    } catch (InvalidProtocolBufferException e) {
      throw new ServiceException(
          ErrorCode.DESERIALIZATION_FAILURE,
          "Error while deserializing the MSP.value.config values",
          e);
    }
    return channelConfigString;
  }

  private String deserializeRootCertsValues(String channelConfigString) {
    List<JSONArray> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_ROOTCERTS);
    valueList.addAll(
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_TLS_ROOT_CERTS));
    Set<String> valueSet = new HashSet<>();

    for (JSONArray jsonArray : valueList) {
      for (Object base64EncodedValueObject : jsonArray) {
        String base64EncodedValueString = (String) base64EncodedValueObject;
        valueSet.add(base64EncodedValueString);
      }
    }
    return deserializeCertificateValues(channelConfigString, valueSet);
  }

  private String deserializeCertificateValues(
      String channelConfigString, Set<String> certValueSet) {
    List<String> valueList =
        JsonPath.parse(channelConfigString).read(FabricClientConstants.JSON_PATH_CERTIFICATE);
    certValueSet.addAll(valueList);
    for (String base64EncodedValueString : certValueSet) {
      String cert = new String(Base64.getDecoder().decode(base64EncodedValueString));
      String base64DecodedString = cert;
      channelConfigString =
          channelConfigString.replace(base64EncodedValueString, base64DecodedString);
    }
    return channelConfigString;
  }
}
