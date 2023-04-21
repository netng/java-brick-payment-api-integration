package id.co.sofcograha.base.utilities.brick;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class BrickAuth {
    private String message;
    private String accessToken;
    private Date issuedAt;
    private Date expiresAt;
}
