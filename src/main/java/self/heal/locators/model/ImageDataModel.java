package self.heal.locators.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@Document("image_data")
public class ImageDataModel extends BaseModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Id
    @Indexed
    private String id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("image_hex_string")
    @Indexed
    private String imageHexId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String base64;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Indexed
    @JsonProperty("healed_element_id")
    private String healedElementId;
    
}
