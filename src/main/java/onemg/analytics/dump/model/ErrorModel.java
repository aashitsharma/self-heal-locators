package onemg.analytics.dump.model;

import lombok.Data;

@Data
public class ErrorModel {
    private int statusCode;
    private String message;

    public ErrorModel errorResp(int statusCode,String message){
        this.statusCode=statusCode;
        this.message=message;
        return this;
    }


}
