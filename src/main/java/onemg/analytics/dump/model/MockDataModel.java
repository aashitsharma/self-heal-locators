package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
@Document("mocked_data")
@CompoundIndexes({
        @CompoundIndex(name = "unique_uri_vertical_method", def = "{'uri' : 1, 'vertical': 1, 'method': 1}", unique = true)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MockDataModel {
    @Id
    private String id;
    private String uri;
    private Map<String, Object> response;
    private String vertical;
    private int responseCode;
    private HttpMethod method;
}
