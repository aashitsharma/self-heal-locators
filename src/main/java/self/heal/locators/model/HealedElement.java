package self.heal.locators.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@CompoundIndexes({
    @CompoundIndex(name = "locator_confidence_idx", def = "{'locator': 1, 'confidenceScore': -1}"),
    @CompoundIndex(
        name = "unique_locator_score",
        def = "{'locator': 1, 'confidenceScore': 1}",
        unique = true
    )
})
@Document("healed_element")
public class HealedElement extends BaseModel{
    
    @Indexed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String locator;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("healed_locator")
    private String healedLocator;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Id
    private String id;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String approach;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String reasoning;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("page_source")
    private String pageSource;

    @Indexed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("model_name")
    private String modelName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("image_id")
    private String imageHexId;
}
