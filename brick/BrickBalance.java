package id.co.sofcograha.base.utilities.brick;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class BrickBalance {
    private Map<String, Object> brickPay;
    private Map<String, Object> brickFlex;
    private Long totalAvailableBalance;
}
