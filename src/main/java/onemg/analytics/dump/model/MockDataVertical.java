package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("mocked_data_vertical")
public class MockDataVertical extends BaseModel{

    @Indexed(unique = true) 
    private String verticalName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Id
    private String id;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String updatedVerticalName;

    
}
