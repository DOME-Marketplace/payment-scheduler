package it.eng.dome.payment.scheduler.service;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.eng.dome.payment.scheduler.config.PrivateKeyLoader;
import it.eng.dome.payment.scheduler.util.M2MTokenUtils;


@Service
public class M2MTokenService {
	
	private static final Logger logger = LoggerFactory.getLogger(M2MTokenService.class);

	private PrivateKeyLoader privateKey;
	private String clientId;
	
	public M2MTokenService(PrivateKeyLoader privateKey) {
		this.privateKey = privateKey;
	}
	
	public Map<String, String> getAssertion(String learCredential) {
		Map<String, String> map = new HashMap<String, String>();
		
		String clientAssertion = createClientAssertion(learCredential);
		//logger.info("clientAssertion: {}", clientAssertion);
		
		if (clientAssertion != null && clientId != null) {
			logger.info("Retrieved clientId and clientAssertion");
			map.put(M2MTokenUtils.CLIENT_ASSERTION, clientAssertion);
			map.put(M2MTokenUtils.CLIENT_ID, clientId);
		}

		return map; 
	}
	
	public String createClientAssertion(String learCredential) {
		
		String jwtCredential = getVCinJWTDecodedFromBase64(learCredential);
		//logger.info("JwtCredential: {}", jwtCredential);
		
		try {
			SignedJWT signedJWT = SignedJWT.parse(jwtCredential);
			Payload vcMachinePayload = signedJWT.getPayload();
			clientId = (String) vcMachinePayload.toJSONObject().get("sub");

			logger.info("Get clientId: {}", clientId);
			
			Instant issueTime = Instant.now();
			long iat = issueTime.toEpochMilli();
			long exp = issueTime.plus(
		        Long.parseLong(M2MTokenUtils.CLIENT_ASSERTION_EXPIRATION),
		        ChronoUnit.valueOf(M2MTokenUtils.CLIENT_ASSERTION_EXPIRATION_UNIT_TIME)
			).toEpochMilli();
			
			String vpTokenJWTString = createVPTokenJWT(jwtCredential, clientId, iat, exp);
			//logger.info("Get VPTokenJWT : {}", vpTokenJWTString);
			
			//ADD: encode vp_token
			String vp_token = Base64.getEncoder().encodeToString(vpTokenJWTString.getBytes());
			//logger.info("Encode vp_token : {}", vpTokenJWTString);
			
			 Payload payload = new Payload(Map.of(
	                "sub", clientId,
	                "iss", clientId,
	                "aud", M2MTokenUtils.AUD,
	                "iat", iat,
	                "exp", exp,
	                "jti", UUID.randomUUID(),
	                "vp_token", vp_token
	        ));
			 
			//logger.info("Payload VP : {}", payload.toString());
			 
			return generateJWT(payload.toString(), clientId);
			
		} catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
			return null;
		}
	}
	
	private String createVPTokenJWT(String jwtCredential, String clientId, long iat, long exp) throws Exception {
        Map<String, Object> vp = createVP(jwtCredential, clientId);

        Payload payload = new Payload(Map.of(
                "sub", clientId,
                "iss", clientId,
                "nbf", iat,
                "iat", iat,
                "exp", exp,
                "jti", UUID.randomUUID(),
                "vp", vp
        ));

        //logger.info("Payload of VPTokenJWT: {}", payload.toString());        
        return generateJWT(payload.toString(), clientId);
	 }
	
	private Map<String, Object> createVP(String jwtCredential, String clientId) {
		return Map.of(
            "@context", List.of("https://www.w3.org/2018/credentials/v1"),
            "holder", clientId,
            "id", "urn:uuid:" + UUID.randomUUID(),
            "type", List.of("VerifiablePresentation"),
            "verifiableCredential", List.of(jwtCredential)
	    );
	}
	
	private String generateJWT(String payload, String clientId) throws Exception {

		String base64String = privateKey.getPrivateKey();
		byte[] decodedBytes = Base64.getDecoder().decode(base64String);
		String hexPrivateKey = bytesToHex(decodedBytes);
      
		try {

			if (hexPrivateKey.startsWith("0x")) {
	            hexPrivateKey = hexPrivateKey.substring(2);
	        }
			
			ECKey ecJWK = buildEcKeyFromPrivateKey(hexPrivateKey, clientId);
			
			// Set Header
            JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(clientId)
                    .type(JOSEObjectType.JWT)
                    .build();
            
            // Set Payload
            JWTClaimsSet claimsSet = convertPayloadToJWTClaimsSet(payload);
            // Create JWT for ES256 algorithm
            SignedJWT jwt = new SignedJWT(jwsHeader, claimsSet);
            // Sign with a private EC key
            JWSSigner signer = new ECDSASigner(ecJWK);
            jwt.sign(signer);
			
			return jwt.serialize();
			
		} catch (Exception e) {
       	 	logger.error("Error creating JWT: {}", e.getMessage());
       	 	throw new Exception(e);
       }
	}
	
	private ECKey buildEcKeyFromPrivateKey(String hexKey, String clientId) {
        try {
            // Convert the private key from hexadecimal string to BigInteger
            BigInteger privateKeyInt = new BigInteger(hexKey, 16);

            // Get the curve parameters for secp256r1 (P-256)
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(M2MTokenUtils.CURVE_NAME);

            // Initialize the key factory for EC algorithm
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderSingleton.getInstance());

            // Create the private key spec for secp256r1
            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, ecSpec);
            ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            // Generate the public key spec from the private key and curve parameters
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecSpec.getG().multiply(privateKeyInt), ecSpec);
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

            // Build the ECKey using secp256r1 curve (P-256)
            return new ECKey.Builder(Curve.P_256, publicKey)
                    .privateKey(privateKey)
                    .keyID(clientId)
                    .build();
            
        } catch (Exception e) {
            logger.error("Error creating JWK source for secp256r1: {}", e.getMessage());
			return null;
        }
    }
	
	private JWTClaimsSet convertPayloadToJWTClaimsSet(String payload) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(payload);
			Map<String, Object> claimsMap = objectMapper.convertValue(jsonNode, new TypeReference<>() {	});
			JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
			for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
				builder.claim(entry.getKey(), entry.getValue());
			}
			return builder.build();
		} catch (JsonProcessingException e) {
			logger.error("Error while parsing the JWT payload: {}", e.getMessage());
			return null;
		}
	}
	
	private String getVCinJWTDecodedFromBase64(String vcTokenBase64) {        
        byte[] vcTokenDecoded = Base64.getDecoder().decode(vcTokenBase64);
        return new String(vcTokenDecoded).replaceAll("\\r|\\n", "");
    }

	private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b)); 
        }
        return hexString.toString();
    }
}
