package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class HealingModel extends BaseModel{

    @JsonProperty("locator")
    private String locator;

    @JsonProperty("page_source")
    private String pageSource;

    private String status;

    @JsonProperty("image_data_id")
    private String imageDataId;
}
