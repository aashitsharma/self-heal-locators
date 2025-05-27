package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaultConfigModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Data{
        public Data data;
        public Metadata metadata;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Metadata{
        public String created_time;
        public String deletion_time;
        public boolean destroyed;
        public int version;
    }


        public String request_id;
        public String lease_id;
        public boolean renewable;
        public int lease_duration;
        public Data data;
        public Object wrap_info;
        public Object warnings;
        public Object auth;
        public String vault_request_id;



}
