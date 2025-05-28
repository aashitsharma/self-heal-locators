package onemg.analytics.dump.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.log4j.MDC;

@Data
public class ErrorModel {
    @JsonProperty("Status Code")
    private int statusCode;
    @JsonProperty("Error Message")
    private String message;
    @JsonProperty("Reference-Id")
    private String refId;

    public ErrorModel errorResp(int statusCode,String message){
        this.statusCode=statusCode;
        this.message=message;
        this.refId=(String) MDC.get("reference");
        return this;
    }


}
