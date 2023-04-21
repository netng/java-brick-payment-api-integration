package id.co.sofcograha.base.utilities.brick;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.co.sofcograha.base.logPemanggilanApiPihakKetiga.services.LogPemanggilanApiPihakKetigaService;
import id.co.sofcograha.base.parentClasses.BaseService;
import id.co.sofcograha.base.utilities.ObjectUtils;
import id.co.sofcograha.base.utilities.StringUtils;
import id.co.sofcograha.gaji.integrasi.masters.objects.integrasi.transferPembayaranBrick.settingTransferPembayaran.entities.MstSettingTransferPembayaran;
import id.co.sofcograha.gaji.integrasi.masters.objects.integrasi.transferPembayaranBrick.settingTransferPembayaran.services.MstSettingTransferPembayaranService;
import id.co.sofcograha.gaji.umum.http.securities.authentications.token.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class BrickService extends BaseService {

    @Autowired
    private MstSettingTransferPembayaranService mstSettingTransferPembayaranService;

    @Autowired
    private LogPemanggilanApiPihakKetigaService logPemanggilanApiPihakKetigaService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${sofcograha.brick.base.url}")
    private String BASE_URL;

    private static final String BASE_URI = "/v2/payments/";
    private static final String BASE_AUTH_URI = "auth/token";
    private static final String API_AUTH_URI = BASE_URI + BASE_AUTH_URI;


    public Map<String, Long> getTotalSaldo() {
        BrickBalanceReponse result = null;
        String method = HttpMethod.GET.toString();
        Map<String, Long> currentBalance = new HashMap<>();
        BrickAuth brickAuth = getPublicToken();

        HttpURLConnection connection = initHttpURLConnection(brickCurrentBalanceEndPointURL(), method);
        connection.setRequestProperty("publicAccessToken", "Bearer " + brickAuth.getAccessToken());

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(connection.getInputStream());
                result = new ObjectMapper().readValue(response.toString(), BrickBalanceReponse.class);
                currentBalance.put("totalSaldo", result.getData().getTotalAvailableBalance());
                logPemanggilanApiPihakKetigaService.addBrickLog(brickCurrentBalanceEndPointURL(), method, null, result);
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                result = new ObjectMapper().readValue(errorResponse.toString(), BrickBalanceReponse.class);
            }
        } catch (IOException e) {
            brickError("Get brick total saldo failed", "brickService.getTotalSaldo.API.call.error",
                    method, null, brickCurrentBalanceEndPointURL(), e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (result != null) {
            if (result.getStatus().equals(String.valueOf(HttpStatus.OK.value()))) {
                return currentBalance;
            } else {
                if (ObjectUtils.isNotEmpty(result.getError())) {
                    brickError("Get saldo failed", "brickService.getTotalSaldo.get.saldo.failed",
                            method, null, brickCurrentBalanceEndPointURL(), result.getError());
                }
            }
        }
        return currentBalance;
    }

    private BrickAuth getPublicToken() {
        BrickAuthResponse result = null;
        BrickCredential brickCredential = brickCredential();
        String publicTokenEndpointURL = BASE_URL + API_AUTH_URI;
        String method = HttpMethod.GET.toString();
        String authString = brickCredential.getClientId() + ":" + brickCredential.getSecretId();
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

        HttpURLConnection connection = initHttpURLConnection(publicTokenEndpointURL, method);
        connection.setRequestProperty("Authorization", authHeader);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(connection.getInputStream());
                result = new ObjectMapper().readValue(response.toString(), BrickAuthResponse.class);
                logPemanggilanApiPihakKetigaService.addBrickLog(publicTokenEndpointURL, method, null, result);
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                result = new ObjectMapper().readValue(errorResponse.toString(), BrickAuthResponse.class);
            }
        } catch (IOException e) {
            brickError("Unable to open connection to endpoint URL", "brickService.getPublicToken.API.call.error",
                    method, null, publicTokenEndpointURL, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (result != null) {
            if (result.getStatus().equals(String.valueOf(HttpStatus.OK.value()))) {
                return result.getData();
            } else {
                if (ObjectUtils.isNotEmpty(result.getError())) {
                    brickError("Request token failed", "brickService.getPublicToken.request.token.failed",
                            method, null, publicTokenEndpointURL, result.getError());
                }
            }
        }
        return result.getData();
    }

    private BrickCredential brickCredential() {
        String companyId = AuthenticatedUser.getCompanyId();
        MstSettingTransferPembayaran settingTransferPembayaran = mstSettingTransferPembayaranService.findByBk(companyId);

        valSettingTransferPembayaranIsExists(settingTransferPembayaran);

        BrickCredential brickCredential = new BrickCredential();
        brickCredential.setClientId(settingTransferPembayaran.getClientId());
        brickCredential.setSecretId(settingTransferPembayaran.getClientSecret());
        return brickCredential;
    }

    private void valSettingTransferPembayaranIsExists(MstSettingTransferPembayaran settingTransferPembayaran) {
        if (ObjectUtils.isEmpty(settingTransferPembayaran)) {
            error("brickService.setting.transfer.pembayaran.not.found");
        }
        valBrickCredentialIsExists(settingTransferPembayaran);
    }

    private void valBrickCredentialIsExists(MstSettingTransferPembayaran settingTransferPembayaran) {
        if (StringUtils.isBlank(settingTransferPembayaran.getClientId())) {
            error("brickService.brick.clientId.not.found");
        }
        if (StringUtils.isBlank(settingTransferPembayaran.getClientId())) {
            error("brickService.brick.clientSecret.not.found");
        }
    }

    private String brickCurrentBalanceEndPointURL() {
        return new StringBuilder(BASE_URL).append(BASE_URI).append("gs/balance").toString();
    }

    private HttpURLConnection initHttpURLConnection(String endpointURL, String method) {
        try {
            URL url = new URL(endpointURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(method);
            return httpURLConnection;
        } catch (MalformedURLException e) {
            brickError("Invalid endpoint URL", "brickService.init.API.call.error.invalid.endpoint",
                    method, null, endpointURL, e.getMessage());
        } catch (IOException e) {
            brickError("Unable to open connection to endpoint URL", "brickService.init.API.call.error.connection.failed",
                    method, null, endpointURL, e.getMessage());
        }
        return null;
    }

    private String readResponse(InputStream inputStream) throws IOException {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        }
    }

    private void brickError(String errorMsg, String errorCode, String method, Object reqHeaderAndParams, String endPointURL, Object responses) {
        String refError = UUID.randomUUID().toString();
        logPemanggilanApiPihakKetigaService.addBrickLog(endPointURL, method, reqHeaderAndParams, responses, refError);
        logger.error(errorMsg + ": " + endPointURL, responses, "ref: " + refError);
        error(errorCode, errorMsg, endPointURL, responses, "ref: " + refError);
    }
}
