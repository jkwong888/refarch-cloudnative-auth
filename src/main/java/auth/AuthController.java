package auth;

import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller to manage Customer database
 *
 */
@RestController
public class AuthController {
    
    private static Logger logger =  LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private CustomerServiceClient customerService;
   
    /**
     * check
     */
    @RequestMapping("/check")
    @ResponseBody String check() {
        return "it works!";
    }
    
    /**
     * @return customer by username
     */
    @RequestMapping(value = "/authenticate", method = RequestMethod.GET)
    @ResponseBody ResponseEntity<?> authenticate(@RequestHeader(value="Authorization", required=false) String authHeader) {
    	logger.info("/authenticate: auth header = " + authHeader);
    	
    	if (authHeader == null) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	// authorization string is like "Basic <base64encoded>"
    	final String creds = authHeader.replace("Basic ", "");
    	
    	if (creds == null || creds.length() == 0) {
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	final String decodedCreds;
    	try {
			decodedCreds = new String(Base64.getDecoder().decode(creds));
    	} catch (Exception e) {
    		// if I can't decode for any reason, HTTP 401
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	final String[] split = decodedCreds.split(":");
    	
    	if (split.length != 2) {
    		// wrong format
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	logger.debug("Authenticating: user=" + split[0] + ", password=" + split[1]);
       
    	// TODO: set signed JWT before calling the customer service?
    	// call customer service
    	final ResponseEntity<List<Customer>> resp = customerService.getCustomerByUsername(split[0]);
    	
    	final List<Customer> custList = resp.getBody();
    	logger.debug("customer service returned:" + custList);
    	
    	if (custList.isEmpty()) {
    		// unknown user
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	final Customer cust = custList.get(0);
    	
    	// TODO: hash password -- in the customer service
    	if (!cust.getPassword().equals(split[1])) {
    		// password doesn't match
    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    	}
    	
    	// write the customer ID to the response in the header: "API-Authenticated-Credential"
    	// this tell API Connect who the access token belongs to/what it corresponds to in
    	// the customer database
    	return ResponseEntity.ok().header("API-Authenticated-Credential", cust.getCustomerId()).build();
    }
    
}
