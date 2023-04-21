package id.co.sofcograha.base.utilities.brick;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrickError {
    private String code;
    private String message;
    private String action;
    private String reason;
}
