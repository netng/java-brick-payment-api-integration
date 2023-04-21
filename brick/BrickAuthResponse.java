package id.co.sofcograha.base.utilities.brick;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class BrickAuthResponse {


    private String status;
    private BrickError error;

    private Map<String, Object> metaData;
    private BrickAuth data;
}
