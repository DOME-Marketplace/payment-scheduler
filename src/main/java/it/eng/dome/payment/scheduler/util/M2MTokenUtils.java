package it.eng.dome.payment.scheduler.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;


public class M2MTokenUtils {
	
	private static final String CLIENT_ASSERTION_EXPIRATION = "5";
	private static final String CLIENT_ASSERTION_EXPIRATION_UNIT_TIME = "MINUTES";
	private static final Logger logger = LoggerFactory.getLogger(M2MTokenUtils.class);
	
	public static String createClientAssertion(String jwtCredential) {
		
		try {
			SignedJWT signedJWT = SignedJWT.parse(jwtCredential);
			Payload vcMachinePayload = signedJWT.getPayload();
			String clientId = (String) vcMachinePayload.toJSONObject().get("sub");
			logger.info("Get clientId : {}", clientId);
			
			Instant issueTime = Instant.now();
			long iat = issueTime.toEpochMilli();
			long exp = issueTime.plus(
		        Long.parseLong(CLIENT_ASSERTION_EXPIRATION),
		        ChronoUnit.valueOf(CLIENT_ASSERTION_EXPIRATION_UNIT_TIME)
			).toEpochMilli();
			
			String vpTokenJWTString = createVPTokenJWT(jwtCredential, clientId, iat, exp);
			logger.info("Get VPTokenJWT : {}", vpTokenJWTString);
			
			//encode vp_token
			String vp_token = Base64.getEncoder().encodeToString(vpTokenJWTString.getBytes());
			logger.info("Encode vp_token : {}", vpTokenJWTString);
			
			 Payload payload = new Payload(Map.of(
	                "sub", clientId,
	                "iss", clientId,
	                "aud", "https://verifier.dome-marketplace-sbx.org",
	                "iat", iat,
	                "exp", exp,
	                "jti", UUID.randomUUID(),
	                "vp_token", vp_token
	        ));
			 
			logger.info("Payload VP : {}", payload.toString());
			 
			return generateJWT(payload.toString(), clientId);
			
		} catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
			return null;
		}
	}
	
	private static String createVPTokenJWT(String jwtCredential, String clientId, long iat, long exp) throws Exception {
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

        logger.info("Payload of VPTokenJWT: {}", payload);
        
        return generateJWT(payload.toString(), clientId);
	 }
	
	private static Map<String, Object> createVP(String jwtCredential, String clientId) {
		return Map.of(
            "@context", List.of("https://www.w3.org/2018/credentials/v1"),
            "holder", clientId,
            "id", "urn:uuid:" + UUID.randomUUID(),
            "type", List.of("VerifiablePresentation"),
            "verifiableCredential", List.of(jwtCredential)
	    );
	}
	
	private static String generateJWT(String payload, String clientId) throws Exception {
		try {
            // Get ECKey
			String hexPrivateKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
			if (hexPrivateKey.startsWith("0x")) {
	            hexPrivateKey = hexPrivateKey.substring(2);
	        }
			
            String curveName = "secp256r1";
            ECPrivateKey ecJWK = getECPrivateKeyFromHex(hexPrivateKey, curveName);
            if (ecJWK != null) {
            	
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
            }else {
            	return null;
            }
        } catch (Exception e) {
        	 logger.error("Error creating JWT: {}", e.getMessage());
            throw new Exception();
        }
	}
	
	private static ECPrivateKey getECPrivateKeyFromHex(String hexKey, String curveName) {

        byte[] keyBytes = hexStringToByteArray(hexKey);
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveName);

        BigInteger privateKeyValue = new BigInteger(1, keyBytes);
        try {
        	Security.addProvider(new BouncyCastleProvider());
	        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyValue, ecSpec);
	        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
	        
	        ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);
			return privateKey;
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
			logger.error("Error: {}", e.getMessage());
			return null;
		}
    }
	
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
	
	private static JWTClaimsSet convertPayloadToJWTClaimsSet(String payload) {
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
	
	public static String getVCinJWTDecodedFromBase64(String vcTokenBase64) {        
        byte[] vcTokenDecoded = Base64.getDecoder().decode(vcTokenBase64);
        return new String(vcTokenDecoded);
    }
}
