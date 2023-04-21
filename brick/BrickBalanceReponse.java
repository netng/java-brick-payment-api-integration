package id.co.sofcograha.base.utilities.brick;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BrickBalanceReponse {
    private Integer status;
    private BrickBalance data;
    private Map<String, Object> metaData;
    private BrickError error;

}
