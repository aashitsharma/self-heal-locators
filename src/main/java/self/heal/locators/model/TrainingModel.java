package self.heal.locators.model;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@Document("training_model")
public class TrainingModel {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String propmt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String completion;
    
}
