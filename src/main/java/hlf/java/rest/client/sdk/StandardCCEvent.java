package hlf.java.rest.client.sdk;

import com.owlike.genson.annotation.JsonProperty;
import lombok.Data;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

/**
 * StandardCCEvent can be used by smart contract developers
 * to send a commonly wrapped event that the hlf-connector
 * decodes. The decoded event can be used to publish to Kafka.
 */
@Data
@DataType
public class StandardCCEvent implements Serializable {
    @Property()
    @JsonProperty("key")
    private String key;

    @Property
    @JsonProperty("event")
    private String event;
}
