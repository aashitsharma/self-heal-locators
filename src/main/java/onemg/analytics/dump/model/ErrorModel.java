package onemg.analytics.dump.model;

import lombok.Data;
import org.apache.log4j.MDC;

@Data
public class ErrorModel {
    private int statusCode;
    private String message;
    private String refId;

    public ErrorModel errorResp(int statusCode,String message){
        this.statusCode=statusCode;
        this.message=message;
        this.refId=(String) MDC.get("reference");
        return this;
    }


}
