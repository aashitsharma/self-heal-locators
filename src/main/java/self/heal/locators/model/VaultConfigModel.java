package self.heal.locators.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultConfigModel extends BaseModel{
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("lease_id")
    private String leaseId;

    @JsonProperty("renewable")
    private boolean renewable;

    @JsonProperty("lease_duration")
    private int leaseDuration;

    @JsonProperty("data")
    private DataWrapper data;

    @JsonProperty("vault_request_id")
    private String vaultRequestId;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataWrapper {
        @JsonProperty("data")
        private Data data;

        @JsonProperty("metadata")
        private Metadata metadata;

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Data {
        @JsonProperty("api")
        private Object api;

        @JsonProperty("app")
        private Object app;

        @JsonProperty("cloud_config")
        private Object cloudConfig;

        @JsonProperty("database")
        private Object database;

        @JsonProperty("execution_details")
        private Object executionDetails;

        @JsonProperty("email")
        private Object email;

        @JsonProperty("config")
        private Object config;

        @JsonProperty("test_data")
        private Object testData;

        @JsonProperty("reportportal")
        private Object reportportal;

        @JsonProperty("common")
        private Object common;

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Metadata {
        @JsonProperty("created_time")
        private String createdTime;

        @JsonProperty("deletion_time")
        private String deletionTime;

        @JsonProperty("destroyed")
        private boolean destroyed;

        @JsonProperty("version")
        private int version;

    }
}
