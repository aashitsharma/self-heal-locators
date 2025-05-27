package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultConfigModel {
    private String request_id;
    private String lease_id;
    private boolean renewable;
    private int lease_duration;
    private VaultData data;
    private Object wrap_info;
    private Object warnings;
    private Object auth;
    private String vault_request_id;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VaultData {
        private Object data; // or use Map<String, Object> if dynamic
        private Metadata metadata;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Metadata {
        private String created_time;
        private String deletion_time;
        private boolean destroyed;
        private int version;
    }
}
