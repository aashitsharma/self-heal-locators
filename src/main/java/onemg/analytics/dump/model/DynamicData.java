package onemg.analytics.dump.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Document(collection = "analytics_event_dump")
public class DynamicData {

    @Id
    private String id;
    private Map<String, Object> data;

    // Constructors, Getters, and Setters
    public DynamicData() {}

    public DynamicData(Map<String, Object> data) {
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
