package it.eng.dome.payment.scheduler.controller;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;


public class GetAssertion {
	
	final String credentials = "eyJhbGciOiJSUzI1NiIsImN0eSI6Impzb24iLCJraWQiOiJNSUhRTUlHM3BJRzBNSUd4TVNJd0lBWURWUVFEREJsRVNVZEpWRVZNSUZSVElFRkVWa0ZPUTBWRUlFTkJJRWN5TVJJd0VBWURWUVFGRXdsQ05EYzBORGMxTmpBeEt6QXBCZ05WQkFzTUlrUkpSMGxVUlV3Z1ZGTWdRMFZTVkVsR1NVTkJWRWxQVGlCQlZWUklUMUpKVkZreEtEQW1CZ05WQkFvTUgwUkpSMGxVUlV3Z1QwNGdWRkpWVTFSRlJDQlRSVkpXU1VORlV5QlRURlV4RXpBUkJnTlZCQWNNQ2xaaGJHeGhaRzlzYVdReEN6QUpCZ05WQkFZVEFrVlRBaFJraVFqbVlLNC95SzlIbGdrVURVNHoyZEo5OWc9PSIsIng1dCNTMjU2IjoidEZHZ19WWHVBdUc3NTZpUG52aWVTWjQ2ajl6S3VINW5TdmJKMHA5cFFaUSIsIng1YyI6WyJNSUlIL1RDQ0JlV2dBd0lCQWdJVVpJa0k1bUN1UDhpdlI1WUpGQTFPTTluU2ZmWXdEUVlKS29aSWh2Y05BUUVOQlFBd2diRXhJakFnQmdOVkJBTU1HVVJKUjBsVVJVd2dWRk1nUVVSV1FVNURSVVFnUTBFZ1J6SXhFakFRQmdOVkJBVVRDVUkwTnpRME56VTJNREVyTUNrR0ExVUVDd3dpUkVsSFNWUkZUQ0JVVXlCRFJWSlVTVVpKUTBGVVNVOU9JRUZWVkVoUFVrbFVXVEVvTUNZR0ExVUVDZ3dmUkVsSFNWUkZUQ0JQVGlCVVVsVlRWRVZFSUZORlVsWkpRMFZUSUZOTVZURVRNQkVHQTFVRUJ3d0tWbUZzYkdGa2IyeHBaREVMTUFrR0ExVUVCaE1DUlZNd0hoY05NalF3TmpJeE1EWTFOelUwV2hjTk1qY3dOakl4TURZMU56VXpXakNCcXpFVk1CTUdBMVVFQXd3TVdrVlZVeUJQVEVsTlVFOVRNUmd3RmdZRFZRUUZFdzlKUkVORlZTMDVPVGs1T1RrNU9WQXhEVEFMQmdOVkJDb01CRnBGVlZNeEVEQU9CZ05WQkFRTUIwOU1TVTFRVDFNeEh6QWRCZ05WQkFzTUZrUlBUVVVnUTNKbFpHVnVkR2xoYkNCSmMzTjFaWEl4R0RBV0JnTlZCR0VNRDFaQlZFVlZMVUk1T1RrNU9UazVPVEVQTUEwR0ExVUVDZ3dHVDB4SlRWQlBNUXN3Q1FZRFZRUUdFd0pGVlRDQ0FpSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnSVBBRENDQWdvQ2dnSUJBTERkMGNGZ3A2dzdqV0dVNW9OU3hBWXVQejlodzMwWHdtQ3AxTldieTh4STBPN2I5blUwT0JwTTR1ZWRDKzdoSDd5Uk51ek9VTzF3S1IwZkpJcVkyc3picTExblZwNnNDTWl1eVlzb0d4NXJNQ3RMM3Y5TFBFdnU2MXhER0xRYVlBZnF0ZjVhTXdHL0QvOTQzdnUvTzJYZWQyc1VOYnIrZDFIYjZlUHVIRzU5ZS9YekRraTBuZUtPOHJSUllRakVlSzhDek50Z3N6NUN4cFBtZ3g5ZUVqMEYwZTEzRjErbzB5VGwzYUhET1FvVUErUWhjQzRYc2UzQkN0TXZnRTl1WTdWKzNlRUhFR2h5bUJjeldtbHVYeGpRMjJDZlREWFZvKzFEa0U3SWhkZU9pdGRBa2txT056VVRzVGwxa2gwTlByNDJaall3K1JaK3EybTI4QTYvbTVEbzBUdGlIaDFML2dHZkVaZjhBRzJUWWt6alhkSGEvdWRFY1hrTmlBeVpGZEo3RDlIYzZwZUhXdlFDZ2VES1dVakVtcExiMkx1c2pqVmRTYTdRc2hZbHZYS3I2b3FRcW5qZ0tOWTMwSXBvOTF2SUxZQ243MTJHRHlMR0x1ZEpxUXI0L0s5Y2cwR21sRUI1OGU4ZHdKRlhXK1o2c3lodW9CaEZESkRZNE9oZnFYeVQ2bnNPOEJ1WVl3YmFMQkFIZGprcmt5UUdpTFJDVk5oTDlBeHdBdXlhRkhjeU5ieXo5RDZ0ZUVXSThSWWFMN2JJNStpa0VBVkVJVWdnZlUxK1JCaFQwa3dDbmVTSk5BYUorSnN2WjA1czFNdTFhakZMWVhZMHI5clVlb1cyMkJDSmJuVXEyYjEzdS92dS9hRlZjTkpMdXE3OXp1YWZJUytybXQ2NUFqN3ZBZ01CQUFHamdnSVBNSUlDQ3pBTUJnTlZIUk1CQWY4RUFqQUFNQjhHQTFVZEl3UVlNQmFBRklJVG9hTUNsTTVpRGVBR3RqZFdRWEJjUmE0ck1IUUdDQ3NHQVFVRkJ3RUJCR2d3WmpBK0JnZ3JCZ0VGQlFjd0FvWXlhSFIwY0RvdkwzQnJhUzVrYVdkcGRHVnNkSE11WlhNdlJFbEhTVlJGVEZSVFVWVkJURWxHU1VWRVEwRkhNUzVqY25Rd0pBWUlLd1lCQlFVSE1BR0dHR2gwZEhBNkx5OXZZM053TG1ScFoybDBaV3gwY3k1bGN6Q0J3QVlEVlIwZ0JJRzRNSUcxTUlHeUJnc3JCZ0VFQVlPblVRb0RDekNCb2pBL0JnZ3JCZ0VGQlFjQ0FSWXphSFIwY0hNNkx5OXdhMmt1WkdsbmFYUmxiSFJ6TG1WekwyUndZeTlFU1VkSlZFVk1WRk5mUkZCRExuWXlMakV1Y0dSbU1GOEdDQ3NHQVFVRkJ3SUNNRk1NVVVObGNuUnBabWxqWVdSdklHTjFZV3hwWm1sallXUnZJR1JsSUdacGNtMWhJR1ZzWldOMGNtOXVhV05oSUdGMllXNTZZV1JoSUdSbElIQmxjbk52Ym1FZ1ptbHphV05oSUhacGJtTjFiR0ZrWVRBUEJna3JCZ0VGQlFjd0FRVUVBZ1VBTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQ0JnZ3JCZ0VGQlFjREJEQkNCZ05WSFI4RU96QTVNRGVnTmFBemhqRm9kSFJ3T2k4dlkzSnNNUzV3YTJrdVpHbG5hWFJsYkhSekxtVnpMMFJVVTFGMVlXeHBabWxsWkVOQlJ6RXVZM0pzTUIwR0ExVWREZ1FXQkJSSnRva0hPWEYyMzVVSktZM0tPQVdhZ1NHZExEQU9CZ05WSFE4QkFmOEVCQU1DQnNBd0RRWUpLb1pJaHZjTkFRRU5CUUFEZ2dJQkFGME1nS1NHWXNiaURrUTVCQmZLc1VGWnpBd2xzTDhrRTYzUHlKMFBMajVzT2VUMEZMWTVJeTVmY0U2NmcwWEozSWsvUG0vYTFiK0hCd2l0bkx3ZGRKbVJwWm9ta09RSWxaYXRUQk9tQTlUd2M4OE5MdU5TdTdVM0F5cXV0akRSbFVDOFpGeWRDY1pUalF0bVVIM1FlU0d4RDYvRy82T0JGK2VVY3o1QTVkenJIMGtKNkQrYTQ3MjBjYitkZ01ycTA0OTBVbTVJcExReXRuOG5qSjNSWWtINnhVNmoxdEJpVmsrTVJ4TUZ6bUoxSlpLd1krd2pFdklidlZrVGt0eGRLWVFubFhGL1g2UlhnZjJ0MEJlK0YyRDU0R3pYcWlxeGMvRVVZM3k1Ni9rTUk1OW5ibGdia1ZPYTZHYVd3aUdPNnk1R3h2MVFlUmxVd2Z5TGZRRFR4Ykh6eXBrUysrcG55NXl2OU5kVytQR2loUVZubGFrdkFUS010M1B4WVZyYU91U3NWQVQyVVlVLy9sRGNJWU44Sk94NDB5amVubVVCci8yWE1yeDd2SzhpbkU1SzI0cmg4OXNZUVc3ZkZLM2RmQTRpeTEzblpRc1RzdWlEWVdBZWV6cTlMU3RObE9ncnFxd0RHRDdwLzRzbFh2RlhwTkxtcjlYaXVWRUtXQ0dmSXJnY0tPck5qV3hRREMwV1NsdGtNUFZTZzVrTlMwTW1GYmM0OHB3WXlmR3o2TkUvSmFVNVFzcXdBNnRtR3FLanhOUXJKRGptYXBheFltL3RYSjZhblhjY2sySWVudDRlc241UDhIdE1uK0wzQWQ0RFF4NWlkVWhPQmtsb1NWVlR2dWUvOXgrZTRQWXJDVHNiT3pBa1VtRTl3amFOSStLNW9jWmFvVEhDQTVDNyJdLCJ0eXAiOiJqb3NlIiwic2lnVCI6IjIwMjUtMDItMjVUMTA6MDE6MjNaIiwiY3JpdCI6WyJzaWdUIl19.eyJzdWIiOiJkaWQ6a2V5OnpEbmFldHdGQzhSekZ1bmVZMldkQ1VmRHFjUTJvNURaU0xoejFMOWZxWVhRUWV1amciLCJuYmYiOjE3NDA0NjgwODgsImlzcyI6ImRpZDplbHNpOlZBVEVVLUI5OTk5OTk5OSIsImV4cCI6MTc3MjAwNDA4OCwiaWF0IjoxNzQwNDY4MDg4LCJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy5ldmlkZW5jZWxlZGdlci5ldS8yMDIyL2NyZWRlbnRpYWxzL21hY2hpbmUvdjEiXSwiaWQiOiI4bDdhNjIxMy01NDRkLTQ1MGQtOGUzZC1iNDFmYTkwMDkxOTkiLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWxNYWNoaW5lIl0sImlzc3VlciI6eyJpZCI6ImRpZDplbHNpOlZBVEVVLUI5OTk5OTk5OSJ9LCJpc3N1YW5jZURhdGUiOiIyMDI0LTAxLTAxVDA4OjAwOjAwLjAwMDAwMDAwMFoiLCJ2YWxpZEZyb20iOiIyMDI1LTAyLTI1VDA4OjAwOjAwLjAwMDAwMDAwMFoiLCJleHBpcmF0aW9uRGF0ZSI6IjIwMjYtMDItMjVUMjM6NTk6MDAuMDAwMDAwMDAwWiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im1hbmRhdGUiOnsiaWQiOiI3YmY1NWQyZS01MjQ3LTQ3MTQtOTFkMS04ZTJmOGNiNzMwZDIiLCJsaWZlX3NwYW4iOnsic3RhcnREYXRlVGltZSI6IjIwMjUtMDItMjVUMDg6MDA6MDAuMDAwMDAwMDAwWiIsImVuZERhdGVUaW1lIjoiMjAyNi0wMi0yNVQyMzo1OTowMC4wMDAwMDAwMDBaIn0sIm1hbmRhdGVlIjp7ImlkIjoiZGlkOmtleTp6RG5hZXR3RkM4UnpGdW5lWTJXZENVZkRxY1EybzVEWlNMaHoxTDlmcVlYUVFldWpnIiwic2VydmljZU5hbWUiOiJwYXltZW50LXNjaGVkdWxlciIsInNlcnZpY2VUeXBlIjoic2NoZWR1bGVyIiwidmVyc2lvbiI6IiIsImRvbWFpbiI6InBheW1lbnQtc2NoZWR1bGVyLXN2Yy5iaWxsaW5nLnN2Yy5jbHVzdGVyLmxvY2FsOjgwODAiLCJpcEFkZHJlc3MiOiIxMC4yMzMuNDIuMTIiLCJkZXNjcmlwdGlvbiI6IlNlcnZpY2Vmb3JwYXltZW50IiwiY29udGFjdCI6eyJlbWFpbCI6InBhc3F1YWxlLnZpdGFsZUBlbmcuaXQiLCJwaG9uZSI6IiszOTM0OTczMDE2NTIifX0sIm1hbmRhdG9yIjp7ImNvbW1vbk5hbWUiOiJGYWJpb01vbW9sYSIsImNvdW50cnkiOiJJVCIsImVtYWlsQWRkcmVzcyI6ImZhYmlvLm1vbW9sYUBlbmcuaXQiLCJvcmdhbml6YXRpb24iOiJFbmdpbmVlcmluZ0luZ2VnbmVyaWFJbmZvcm1hdGljYVMucC5BLiIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRJVC0wNTcyNDgzMTAwMiIsInNlcmlhbE51bWJlciI6IjA1NzI0ODMxMDAyIn0sInBvd2VyIjpbeyJpZCI6IjFhMjY2ODY5LTljZGEtNDJjNC04ODRmLWJkMThhNzllOGJmeCIsImRvbWFpbiI6IkRPTUUiLCJmdW5jdGlvbiI6IkxvZ2luIiwiYWN0aW9uIjoib2lkY19tMm0ifV0sInNpZ25lciI6eyJjb21tb25OYW1lIjoiWkVVU09MSU1QT1MiLCJjb3VudHJ5IjoiRVMiLCJlbWFpbEFkZHJlc3MiOiJkb21lc3VwcG9ydEBpbjIuZXMiLCJvcmdhbml6YXRpb24iOiJJTjIiLCJvcmdhbml6YXRpb25JZGVudGlmaWVyIjoiVkFURVUtQjk5OTk5OTk5Iiwic2VyaWFsTnVtYmVyIjoiSURDRVUtOTk5OTk5OTlQIn19fX0sImp0aSI6Ijc0NTkzOWMwLTRkYmYtNDUxYi1hMjEzLTMyZGY0ZjA3YzI2MyJ9.S1Cbi94YzHcvJbjWwk_nXY_saOxJVjwI8urwqd_jDRV_LqsKI395D9VI9gBQFrc0IBmgnUsBAov98QUUZQk68kDj05W-hlbXl_1RmH7krlpJLzy4ZaNElGyE3nvCJKG1oM3xbeADFmoph1A2gQnuSXpxQ7BUl8UHcBOmXAW2m91fEBnFpZa1LQMXWRFsOx9ZUy7V9Sz78rnQVm3AjI8qtllCefA6S17f3KgqcLkrCbqTHrXwWGzMcT2LnQtXiBABF1aiRngXcPkkRzkark-YDQrTdrziODGN1djVBXMc8SUQWv_j-w0GFE2pf-lkBsQvO6mWBuCRuXfQgX52oUWDEN4yGRCM4cZkqIUGb90KOc4RQonn6qs3xCEMy721HsjXOFBaBWm_EmsspApUyEH1--hgv_Gca4jDcLInZsemjs-uXtpFBeXGhOSE1O3QbzbtnRoUiXFdB83LxFOzYgkoiXW4Jdr6JI_fjTjanaG8IE6MrKvdODNAfHDsj0Ab7mfoHCYntQBxuL3hXmnRJO7WTSRtBrcsm3UjMFOl6ul4aeydHudmZbUsT3DfQ72Qf4k7FSLVfuXeVMVh5tjIlHm37YtkO2Ou0OprRlFBAjVdyyvzSXt1yJVJ-PneTB5Pawc2FQKPDq5Zi8qp2dyhhoBT8FkF66sZHD25XrFNSNyssfc";
	String clientId = "did:key:zDnaetwFC8RzFuneY2WdCUfDqcQ2o5DZSLhz1L9fqYXQQeujg";
	
	public static void main(String[] args) {
		
		GetAssertion assertion = new GetAssertion();
		assertion.createClientAssertion();
		
		//assertion.test();
		
		//System.out.println(assertion.createVP(vcMachineString, clientId));

		//String vpTokenJWTString = createVPTokenJWT(vcMachineString, clientId, iat, exp);
	}
	
	private String createClientAssertion() {
		
		try {
			String vcMachineString = credentials;
			
			SignedJWT signedJWT = SignedJWT.parse(credentials);
			Payload vcMachinePayload = signedJWT.getPayload();
			String clientId = (String) vcMachinePayload.toJSONObject().get("sub");
			
			System.out.println("clientId: "+ clientId);
			
			Instant issueTime = Instant.now();
			long iat = issueTime.toEpochMilli();
			long exp = issueTime.plus(
			        Long.parseLong("5"),
			        ChronoUnit.valueOf("MINUTES")
			).toEpochMilli();
			
			String vpTokenJWTString = createVPTokenJWT(vcMachineString, clientId, iat, exp);
			System.out.println("vpTokenJWTString: " + vpTokenJWTString);
			
			
		} catch (ParseException | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "ok";
	}
	
	private String createClientAssertion1() {
        String vcMachineString = getVCinJWTDecodedFromBase64();
        System.out.println(vcMachineString);
        
        Instant issueTime = Instant.now();
		long iat = issueTime.toEpochMilli();
		long exp = issueTime.plus(
		        Long.parseLong("5"),
		        ChronoUnit.valueOf("MINUTES")
		).toEpochMilli();

		
		Map<String, Object> vp = createVP(vcMachineString, clientId);
		
		Payload payload = new Payload(Map.of(
                "sub", clientId,
                "iss", clientId,
                "nbf", iat,
                "iat", iat,
                "exp", exp,
                "jti", UUID.randomUUID(),
                "vp", vp
        ));
        
		System.out.println(payload.toString());
		
		getInfoSignedJWT();
		
		//String vpTokenJWT = generateJWT(payload.toString());
		//System.out.println(vpTokenJWT);
		
		//String vpTokenJWTString = createVPTokenJWT(vcMachineString, clientId, iat, exp);
        //System.out.println(vpTokenJWTString);
        return null;
	}
	

	  private String getVCinJWTDecodedFromBase64() {
		    //String vcTokenBase64 = "eyJhbGciOiJSUzI1NiIsImN0eSI6Impzb24iLCJraWQiOiJNSUhRTUlHM3BJRzBNSUd4TVNJd0lBWURWUVFEREJsRVNVZEpWRVZNSUZSVElFRkVWa0ZPUTBWRUlFTkJJRWN5TVJJd0VBWURWUVFGRXdsQ05EYzBORGMxTmpBeEt6QXBCZ05WQkFzTUlrUkpSMGxVUlV3Z1ZGTWdRMFZTVkVsR1NVTkJWRWxQVGlCQlZWUklUMUpKVkZreEtEQW1CZ05WQkFvTUgwUkpSMGxVUlV3Z1QwNGdWRkpWVTFSRlJDQlRSVkpXU1VORlV5QlRURlV4RXpBUkJnTlZCQWNNQ2xaaGJHeGhaRzlzYVdReEN6QUpCZ05WQkFZVEFrVlRBaFJraVFqbVlLNC95SzlIbGdrVURVNHoyZEo5OWc9PSIsIng1dCNTMjU2IjoidEZHZ19WWHVBdUc3NTZpUG52aWVTWjQ2ajl6S3VINW5TdmJKMHA5cFFaUSIsIng1YyI6WyJNSUlIL1RDQ0JlV2dBd0lCQWdJVVpJa0k1bUN1UDhpdlI1WUpGQTFPTTluU2ZmWXdEUVlKS29aSWh2Y05BUUVOQlFBd2diRXhJakFnQmdOVkJBTU1HVVJKUjBsVVJVd2dWRk1nUVVSV1FVNURSVVFnUTBFZ1J6SXhFakFRQmdOVkJBVVRDVUkwTnpRME56VTJNREVyTUNrR0ExVUVDd3dpUkVsSFNWUkZUQ0JVVXlCRFJWSlVTVVpKUTBGVVNVOU9JRUZWVkVoUFVrbFVXVEVvTUNZR0ExVUVDZ3dmUkVsSFNWUkZUQ0JQVGlCVVVsVlRWRVZFSUZORlVsWkpRMFZUSUZOTVZURVRNQkVHQTFVRUJ3d0tWbUZzYkdGa2IyeHBaREVMTUFrR0ExVUVCaE1DUlZNd0hoY05NalF3TmpJeE1EWTFOelUwV2hjTk1qY3dOakl4TURZMU56VXpXakNCcXpFVk1CTUdBMVVFQXd3TVdrVlZVeUJQVEVsTlVFOVRNUmd3RmdZRFZRUUZFdzlKUkVORlZTMDVPVGs1T1RrNU9WQXhEVEFMQmdOVkJDb01CRnBGVlZNeEVEQU9CZ05WQkFRTUIwOU1TVTFRVDFNeEh6QWRCZ05WQkFzTUZrUlBUVVVnUTNKbFpHVnVkR2xoYkNCSmMzTjFaWEl4R0RBV0JnTlZCR0VNRDFaQlZFVlZMVUk1T1RrNU9UazVPVEVQTUEwR0ExVUVDZ3dHVDB4SlRWQlBNUXN3Q1FZRFZRUUdFd0pGVlRDQ0FpSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnSVBBRENDQWdvQ2dnSUJBTERkMGNGZ3A2dzdqV0dVNW9OU3hBWXVQejlodzMwWHdtQ3AxTldieTh4STBPN2I5blUwT0JwTTR1ZWRDKzdoSDd5Uk51ek9VTzF3S1IwZkpJcVkyc3picTExblZwNnNDTWl1eVlzb0d4NXJNQ3RMM3Y5TFBFdnU2MXhER0xRYVlBZnF0ZjVhTXdHL0QvOTQzdnUvTzJYZWQyc1VOYnIrZDFIYjZlUHVIRzU5ZS9YekRraTBuZUtPOHJSUllRakVlSzhDek50Z3N6NUN4cFBtZ3g5ZUVqMEYwZTEzRjErbzB5VGwzYUhET1FvVUErUWhjQzRYc2UzQkN0TXZnRTl1WTdWKzNlRUhFR2h5bUJjeldtbHVYeGpRMjJDZlREWFZvKzFEa0U3SWhkZU9pdGRBa2txT056VVRzVGwxa2gwTlByNDJaall3K1JaK3EybTI4QTYvbTVEbzBUdGlIaDFML2dHZkVaZjhBRzJUWWt6alhkSGEvdWRFY1hrTmlBeVpGZEo3RDlIYzZwZUhXdlFDZ2VES1dVakVtcExiMkx1c2pqVmRTYTdRc2hZbHZYS3I2b3FRcW5qZ0tOWTMwSXBvOTF2SUxZQ243MTJHRHlMR0x1ZEpxUXI0L0s5Y2cwR21sRUI1OGU4ZHdKRlhXK1o2c3lodW9CaEZESkRZNE9oZnFYeVQ2bnNPOEJ1WVl3YmFMQkFIZGprcmt5UUdpTFJDVk5oTDlBeHdBdXlhRkhjeU5ieXo5RDZ0ZUVXSThSWWFMN2JJNStpa0VBVkVJVWdnZlUxK1JCaFQwa3dDbmVTSk5BYUorSnN2WjA1czFNdTFhakZMWVhZMHI5clVlb1cyMkJDSmJuVXEyYjEzdS92dS9hRlZjTkpMdXE3OXp1YWZJUytybXQ2NUFqN3ZBZ01CQUFHamdnSVBNSUlDQ3pBTUJnTlZIUk1CQWY4RUFqQUFNQjhHQTFVZEl3UVlNQmFBRklJVG9hTUNsTTVpRGVBR3RqZFdRWEJjUmE0ck1IUUdDQ3NHQVFVRkJ3RUJCR2d3WmpBK0JnZ3JCZ0VGQlFjd0FvWXlhSFIwY0RvdkwzQnJhUzVrYVdkcGRHVnNkSE11WlhNdlJFbEhTVlJGVEZSVFVWVkJURWxHU1VWRVEwRkhNUzVqY25Rd0pBWUlLd1lCQlFVSE1BR0dHR2gwZEhBNkx5OXZZM053TG1ScFoybDBaV3gwY3k1bGN6Q0J3QVlEVlIwZ0JJRzRNSUcxTUlHeUJnc3JCZ0VFQVlPblVRb0RDekNCb2pBL0JnZ3JCZ0VGQlFjQ0FSWXphSFIwY0hNNkx5OXdhMmt1WkdsbmFYUmxiSFJ6TG1WekwyUndZeTlFU1VkSlZFVk1WRk5mUkZCRExuWXlMakV1Y0dSbU1GOEdDQ3NHQVFVRkJ3SUNNRk1NVVVObGNuUnBabWxqWVdSdklHTjFZV3hwWm1sallXUnZJR1JsSUdacGNtMWhJR1ZzWldOMGNtOXVhV05oSUdGMllXNTZZV1JoSUdSbElIQmxjbk52Ym1FZ1ptbHphV05oSUhacGJtTjFiR0ZrWVRBUEJna3JCZ0VGQlFjd0FRVUVBZ1VBTUIwR0ExVWRKUVFXTUJRR0NDc0dBUVVGQndNQ0JnZ3JCZ0VGQlFjREJEQkNCZ05WSFI4RU96QTVNRGVnTmFBemhqRm9kSFJ3T2k4dlkzSnNNUzV3YTJrdVpHbG5hWFJsYkhSekxtVnpMMFJVVTFGMVlXeHBabWxsWkVOQlJ6RXVZM0pzTUIwR0ExVWREZ1FXQkJSSnRva0hPWEYyMzVVSktZM0tPQVdhZ1NHZExEQU9CZ05WSFE4QkFmOEVCQU1DQnNBd0RRWUpLb1pJaHZjTkFRRU5CUUFEZ2dJQkFGME1nS1NHWXNiaURrUTVCQmZLc1VGWnpBd2xzTDhrRTYzUHlKMFBMajVzT2VUMEZMWTVJeTVmY0U2NmcwWEozSWsvUG0vYTFiK0hCd2l0bkx3ZGRKbVJwWm9ta09RSWxaYXRUQk9tQTlUd2M4OE5MdU5TdTdVM0F5cXV0akRSbFVDOFpGeWRDY1pUalF0bVVIM1FlU0d4RDYvRy82T0JGK2VVY3o1QTVkenJIMGtKNkQrYTQ3MjBjYitkZ01ycTA0OTBVbTVJcExReXRuOG5qSjNSWWtINnhVNmoxdEJpVmsrTVJ4TUZ6bUoxSlpLd1krd2pFdklidlZrVGt0eGRLWVFubFhGL1g2UlhnZjJ0MEJlK0YyRDU0R3pYcWlxeGMvRVVZM3k1Ni9rTUk1OW5ibGdia1ZPYTZHYVd3aUdPNnk1R3h2MVFlUmxVd2Z5TGZRRFR4Ykh6eXBrUysrcG55NXl2OU5kVytQR2loUVZubGFrdkFUS010M1B4WVZyYU91U3NWQVQyVVlVLy9sRGNJWU44Sk94NDB5amVubVVCci8yWE1yeDd2SzhpbkU1SzI0cmg4OXNZUVc3ZkZLM2RmQTRpeTEzblpRc1RzdWlEWVdBZWV6cTlMU3RObE9ncnFxd0RHRDdwLzRzbFh2RlhwTkxtcjlYaXVWRUtXQ0dmSXJnY0tPck5qV3hRREMwV1NsdGtNUFZTZzVrTlMwTW1GYmM0OHB3WXlmR3o2TkUvSmFVNVFzcXdBNnRtR3FLanhOUXJKRGptYXBheFltL3RYSjZhblhjY2sySWVudDRlc241UDhIdE1uK0wzQWQ0RFF4NWlkVWhPQmtsb1NWVlR2dWUvOXgrZTRQWXJDVHNiT3pBa1VtRTl3amFOSStLNW9jWmFvVEhDQTVDNyJdLCJ0eXAiOiJqb3NlIiwic2lnVCI6IjIwMjUtMDItMjVUMTA6MDE6MjNaIiwiY3JpdCI6WyJzaWdUIl19";
	        String vcTokenBase64 = "eyJzdWIiOiJkaWQ6a2V5OnpEbmFldHdGQzhSekZ1bmVZMldkQ1VmRHFjUTJvNURaU0xoejFMOWZxWVhRUWV1amciLCJuYmYiOjE3NDA0NjgwODgsImlzcyI6ImRpZDplbHNpOlZBVEVVLUI5OTk5OTk5OSIsImV4cCI6MTc3MjAwNDA4OCwiaWF0IjoxNzQwNDY4MDg4LCJ2YyI6eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy5ldmlkZW5jZWxlZGdlci5ldS8yMDIyL2NyZWRlbnRpYWxzL21hY2hpbmUvdjEiXSwiaWQiOiI4bDdhNjIxMy01NDRkLTQ1MGQtOGUzZC1iNDFmYTkwMDkxOTkiLCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWxNYWNoaW5lIl0sImlzc3VlciI6eyJpZCI6ImRpZDplbHNpOlZBVEVVLUI5OTk5OTk5OSJ9LCJpc3N1YW5jZURhdGUiOiIyMDI0LTAxLTAxVDA4OjAwOjAwLjAwMDAwMDAwMFoiLCJ2YWxpZEZyb20iOiIyMDI1LTAyLTI1VDA4OjAwOjAwLjAwMDAwMDAwMFoiLCJleHBpcmF0aW9uRGF0ZSI6IjIwMjYtMDItMjVUMjM6NTk6MDAuMDAwMDAwMDAwWiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7Im1hbmRhdGUiOnsiaWQiOiI3YmY1NWQyZS01MjQ3LTQ3MTQtOTFkMS04ZTJmOGNiNzMwZDIiLCJsaWZlX3NwYW4iOnsic3RhcnREYXRlVGltZSI6IjIwMjUtMDItMjVUMDg6MDA6MDAuMDAwMDAwMDAwWiIsImVuZERhdGVUaW1lIjoiMjAyNi0wMi0yNVQyMzo1OTowMC4wMDAwMDAwMDBaIn0sIm1hbmRhdGVlIjp7ImlkIjoiZGlkOmtleTp6RG5hZXR3RkM4UnpGdW5lWTJXZENVZkRxY1EybzVEWlNMaHoxTDlmcVlYUVFldWpnIiwic2VydmljZU5hbWUiOiJwYXltZW50LXNjaGVkdWxlciIsInNlcnZpY2VUeXBlIjoic2NoZWR1bGVyIiwidmVyc2lvbiI6IiIsImRvbWFpbiI6InBheW1lbnQtc2NoZWR1bGVyLXN2Yy5iaWxsaW5nLnN2Yy5jbHVzdGVyLmxvY2FsOjgwODAiLCJpcEFkZHJlc3MiOiIxMC4yMzMuNDIuMTIiLCJkZXNjcmlwdGlvbiI6IlNlcnZpY2Vmb3JwYXltZW50IiwiY29udGFjdCI6eyJlbWFpbCI6InBhc3F1YWxlLnZpdGFsZUBlbmcuaXQiLCJwaG9uZSI6IiszOTM0OTczMDE2NTIifX0sIm1hbmRhdG9yIjp7ImNvbW1vbk5hbWUiOiJGYWJpb01vbW9sYSIsImNvdW50cnkiOiJJVCIsImVtYWlsQWRkcmVzcyI6ImZhYmlvLm1vbW9sYUBlbmcuaXQiLCJvcmdhbml6YXRpb24iOiJFbmdpbmVlcmluZ0luZ2VnbmVyaWFJbmZvcm1hdGljYVMucC5BLiIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRJVC0wNTcyNDgzMTAwMiIsInNlcmlhbE51bWJlciI6IjA1NzI0ODMxMDAyIn0sInBvd2VyIjpbeyJpZCI6IjFhMjY2ODY5LTljZGEtNDJjNC04ODRmLWJkMThhNzllOGJmeCIsImRvbWFpbiI6IkRPTUUiLCJmdW5jdGlvbiI6IkxvZ2luIiwiYWN0aW9uIjoib2lkY19tMm0ifV0sInNpZ25lciI6eyJjb21tb25OYW1lIjoiWkVVU09MSU1QT1MiLCJjb3VudHJ5IjoiRVMiLCJlbWFpbEFkZHJlc3MiOiJkb21lc3VwcG9ydEBpbjIuZXMiLCJvcmdhbml6YXRpb24iOiJJTjIiLCJvcmdhbml6YXRpb25JZGVudGlmaWVyIjoiVkFURVUtQjk5OTk5OTk5Iiwic2VyaWFsTnVtYmVyIjoiSURDRVUtOTk5OTk5OTlQIn19fX0sImp0aSI6Ijc0NTkzOWMwLTRkYmYtNDUxYi1hMjEzLTMyZGY0ZjA3YzI2MyJ9";
		    //String vcTokenBase64 = "S1Cbi94YzHcvJbjWwk_nXY_saOxJVjwI8urwqd_jDRV_LqsKI395D9VI9gBQFrc0IBmgnUsBAov98QUUZQk68kDj05W-hlbXl_1RmH7krlpJLzy4ZaNElGyE3nvCJKG1oM3xbeADFmoph1A2gQnuSXpxQ7BUl8UHcBOmXAW2m91fEBnFpZa1LQMXWRFsOx9ZUy7V9Sz78rnQVm3AjI8qtllCefA6S17f3KgqcLkrCbqTHrXwWGzMcT2LnQtXiBABF1aiRngXcPkkRzkark-YDQrTdrziODGN1djVBXMc8SUQWv_j-w0GFE2pf-lkBsQvO6mWBuCRuXfQgX52oUWDEN4yGRCM4cZkqIUGb90KOc4RQonn6qs3xCEMy721HsjXOFBaBWm_EmsspApUyEH1--hgv_Gca4jDcLInZsemjs-uXtpFBeXGhOSE1O3QbzbtnRoUiXFdB83LxFOzYgkoiXW4Jdr6JI_fjTjanaG8IE6MrKvdODNAfHDsj0Ab7mfoHCYntQBxuL3hXmnRJO7WTSRtBrcsm3UjMFOl6ul4aeydHudmZbUsT3DfQ72Qf4k7FSLVfuXeVMVh5tjIlHm37YtkO2Ou0OprRlFBAjVdyyvzSXt1yJVJ-PneTB5Pawc2FQKPDq5Zi8qp2dyhhoBT8FkF66sZHD25XrFNSNyssfc";
		    
	        System.out.println(vcTokenBase64);
	        byte[] vcTokenDecoded = Base64.getDecoder().decode(vcTokenBase64);
	        return new String(vcTokenDecoded);
	    }
	
	
	public void test() {
		String[] parts = credentials.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token non valido");
        }

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        System.out.println("Header JSON: " + headerJson);
        
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        System.out.println("Payload JSON: " + payloadJson);
        
        String certificate = new String(Base64.getUrlDecoder().decode(parts[2]));
        System.out.println("signature: " + certificate);
	}
	
	public String getPrivateKey() {
        String privateKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        return privateKey;
    }
	
	/*
	private ECKey buildEcKeyFromPrivateKey() {
        try {
            // Convert the private key from hexadecimal string to BigInteger
            BigInteger privateKeyInt = new BigInteger(getPrivateKey(), 16);

            // Get the curve parameters for secp256r1 (P-256)
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");

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
                    .keyID(generateDidKey(publicKey))
                    .build();
        } catch (Exception e) {
            throw new ECKeyCreationException("Error creating JWK source for secp256r1: " + e);
        }
    }*/

	/*
	private PrivateKey getPrivateKey() {
		String hexKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
		byte[] keyBytes = HexUtils.hexStringToByteArray(hexKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Usa "EC" per chiavi curve ellittiche
        return keyFactory.generatePrivate(spec);
	}
	*/
	 private String createVPTokenJWT(String vcMachineString, String clientId, long iat, long exp) throws InvalidKeySpecException {
        Map<String, Object> vp = createVP(vcMachineString, clientId);

        Payload payload = new Payload(Map.of(
                "sub", clientId,
                "iss", clientId,
                "nbf", iat,
                "iat", iat,
                "exp", exp,
                "jti", UUID.randomUUID(),
                "vp", vp
        ));
        System.out.println("payload createVPTokenJWT: " + payload);
        return generateJWT(payload.toString());
	 }
	 
	 private Map<String, Object> createVP(String vcMachineString, String clientId) {
        return Map.of(
            "@context", List.of("https://www.w3.org/2018/credentials/v1"),
            "holder", clientId,
            "id", "urn:uuid:" + UUID.randomUUID(),
            "type", List.of("VerifiablePresentation"),
            "verifiableCredential", List.of(vcMachineString)
        );
    }
	 
	private String generateJWT3(String payload) {

		// Set Header
		JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
				.keyID("did:key:zDnaeqFhw3mr6LNH8Wffyz3ZKBo2SA4hr1qbvpcUK2NRTcWLX")
				.type(JOSEObjectType.JWT)
				.build();
		
		// Set Payload
		JWTClaimsSet claimsSet = convertPayloadToJWTClaimsSet(payload);
		// Create JWT for ES256 algorithm
		SignedJWT jwt = new SignedJWT(jwsHeader, claimsSet);
		// Sign with a private EC key
		// JWSSigner signer = new ECDSASigner(ecJWK);
		//JWSSigner signer = new RSASSASigner(privateKey);
		//jwt.sign(signer);

		return null;
	}
	
	 private void getInfoSignedJWT() {
	
		 try {
			SignedJWT signedJWT = SignedJWT.parse(credentials);
			
			System.out.println("Payload: " + signedJWT.getPayload().toString());
			Payload vcMachinePayload = signedJWT.getPayload();
			System.out.println(">>> " + vcMachinePayload.toJSONObject().get("sub"));
			
			
			System.out.println("Header: " + signedJWT.getHeader().getKeyID());

			System.out.println("Signature: " + signedJWT.getSignature());
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	 }
	 
	 public ECKey getECKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
		 BigInteger privateKeyInt = new BigInteger(getPrivateKey(), 16);

         // Get the curve parameters for secp256r1 (P-256)
         ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");

         // Initialize the key factory for EC algorithm
         KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderSingleton.getInstance());

         // Create the private key spec for secp256r1
         ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, ecSpec);
         ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

         // Generate the public key spec from the private key and curve parameters
         ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecSpec.getG().multiply(privateKeyInt), ecSpec);
         ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

         // Build the ECKey using secp256r1 curve (P-256)
         /*return new ECKey.Builder(Curve.P_256, publicKey)
                 .privateKey(privateKey)
                 .keyID(generateDidKey(publicKey))
                 .build();
         */
         new ECKey.Builder(Curve.P_256, publicKey);
         return null;
	 }
	 
	 private PrivateKey getPrivateKeyFromHex() {
		 String hexKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
		 if (hexKey.startsWith("0x")) {
            hexKey = hexKey.substring(2);
        }

        // Convertire la stringa esadecimale in un array di byte
        byte[] keyBytes = new byte[hexKey.length() / 2];
        for (int i = 0; i < hexKey.length(); i += 2) {
            keyBytes[i / 2] = (byte) ((Character.digit(hexKey.charAt(i), 16) << 4)
                    + Character.digit(hexKey.charAt(i + 1), 16));
        }

        // Creiamo un PKCS8EncodedKeySpec con i byte della chiave
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        // Usare KeyFactory per creare la chiave privata EC
        KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("EC");
			return keyFactory.generatePrivate(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // "EC" per chiavi EC
        return null;
	 }
	 
	 private String generateJWT(String payload) {

		// 1. Caricare la chiave privata RSA
		/*String hexKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
		if (hexKey.startsWith("0x")) {
			hexKey = hexKey.substring(2); // Rimuove il prefisso "0x"
        }*/

		//byte[] keyBytes = new BigInteger(hexKey, 16).toByteArray();
        //byte[] keyBytes = Files.readAllBytes(Paths.get("private_key.pem"));
		
		/*byte[] keyBytes = new byte[hexKey.length() / 2];
        for (int i = 0; i < hexKey.length(); i += 2) {
            keyBytes[i / 2] = (byte) ((Character.digit(hexKey.charAt(i), 16) << 4)
                    + Character.digit(hexKey.charAt(i + 1), 16));
        }*/
		/*BigInteger privateKeyInt = new BigInteger(getPrivateKey(), 16);
        
     // Get the curve parameters for secp256r1 (P-256)
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
        
        //PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        ECPrivateKeySpec spec = new ECPrivateKeySpec(privateKeyInt, ecSpec);
        
        */
		try {
			String hexKey = "0xc8eb2db3c873acc6af96fdf1f85fd41a2871bc01bcf512dfad071f2d6f55afa1";
		
			PrivateKey privateKey = getPrivateKeyFromHex(hexKey);
	        System.out.println("privateKey: " + privateKey);
	        
	       // Set Header
	       JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.ES256)
	                .keyID("did:key:zDnaeqFhw3mr6LNH8Wffyz3ZKBo2SA4hr1qbvpcUK2NRTcWLX")
	                .type(JOSEObjectType.JWT)
	                .build();
	        
	       // Set Payload
	       JWTClaimsSet claimsSet = convertPayloadToJWTClaimsSet(payload);
	       // Create JWT for ES256 algorithm
	       SignedJWT jwt = new SignedJWT(jwsHeader, claimsSet);
	       // Sign with a private EC key
	       //JWSSigner signer = new ECDSASigner(ecJWK);
	       JWSSigner signer = new RSASSASigner(privateKey);
	       jwt.sign(signer);
	       
	    // 4. Convertire in stringa il JWT firmato
	        String jwtString = jwt.serialize();

	        // 5. Stampare il JWT
	        System.out.println("JWT generato: " + jwt);
	        
	        return jwtString;
	       
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
       
       /*
        // 2. Creare il JWT
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .issuer("my-app")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000)) // 1 minuto di validità
                .build();

        // 3. Creare l'intestazione e firmare
        JWSHeader header = new JWSHeader(JWSAlgorithm.ES256);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);*/

        return null;
	}
	 
	 public PrivateKey getPrivateKeyFromHex(String hexKey) throws Exception {
        // Converte la chiave hex in un array di byte
        byte[] keyBytes = hexStringToByteArray(hexKey);
        
        // Carica la chiave privata utilizzando KeyFactory
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        
        return privateKey;
    }
	 
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
		return data;
	}

	private JWTClaimsSet convertPayloadToJWTClaimsSet(String payload) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(payload);
			Map<String, Object> claimsMap = objectMapper.convertValue(jsonNode, new TypeReference<>() {
			});
			JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
			for (Map.Entry<String, Object> entry : claimsMap.entrySet()) {
				builder.claim(entry.getKey(), entry.getValue());
			}
			return builder.build();
		} catch (JsonProcessingException e) {
			System.out.println("Error while parsing the JWT payload: " + e.getMessage());
			// throw new JWTCreationException("Error while parsing the JWT payload");
			return null;
		}

	}
	
}
