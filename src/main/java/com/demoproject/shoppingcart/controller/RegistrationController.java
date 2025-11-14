package com.demoproject.shoppingcart.controller;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.demoproject.shoppingcart.model.User;
import com.demoproject.shoppingcart.service.RegistrationService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RestController
public class RegistrationController {

	@Autowired
	private RegistrationService service;
	
	public String encrypt(String str){
//       StringBuffer sb = new StringBuffer();
//      char ch[] = str.toCharArray();
//      for(int i = 0; i < ch.length; i++) {
//         String hexString = Integer.toHexString(ch[i]);
//         sb.append(hexString);
//      }
//      String result = sb.toString();
//      return result;
		Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(str.getBytes());
    }

	@PostMapping("/registeruser")
	@CrossOrigin(origins = "http://localhost:4200")
	public User registerUser(@RequestBody User user) throws Exception {
		user.setRole("ROLE_USER");
		String tempEmailId = user.getEmailId();
		if (tempEmailId != null && !"".equals(tempEmailId)) {
			User obj = service.fetchUserByEmailId(tempEmailId);
			if (obj != null)
				throw new Exception("User with " + tempEmailId + " already exists!");
		}
		User obj = null;
//		String temppass=this.encrypt(user.getPassword());
//		user.setPassword(temppass);
		obj = service.saveUser(user);
		return obj;
	}

	private static final String SECRET_KEY="oGp4NeWu0OBBmcyVF36Q_YVkF5TYkuNHaFwlCYuc-jA";
	@PostMapping("/login")
	@CrossOrigin(origins = "http://localhost:4200")
	public String loginUser(@RequestBody User user) throws Exception {
		String tempEmailId = user.getEmailId();
		
		// for front-end password encryption there is no need to encrypt the password at the back-end
//		String tempPass = this.encrypt(user.getPassword());
		String tempPass = user.getPassword();
		System.out.println(tempPass);
		User userObj = null;
		if (tempEmailId != null && tempPass != null) {
			userObj = service.fetchUserByEmailIdAndPassword(tempEmailId, tempPass);
		}
		if (userObj == null)
			throw new Exception("Bad Credentials");
		
		long time = System.currentTimeMillis();
		Date issuedTime = new Date(time);
		Date expireTime = new Date(time + 1800000);
		
		String token = Jwts.builder()
						.setIssuedAt(issuedTime)
						.claim("id", userObj.getId())
						.claim("emailId", userObj.getEmailId())
						.claim("userName", userObj.getUserName())
						.claim("role", userObj.getRole())
						.setExpiration(expireTime)
						.signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
		System.out.println(token);
		
		return token;
//		return userObj;
	}
	
	@PostMapping("/updateuser")
	@CrossOrigin(origins = "http://localhost:4200")
	public User updateUser(@RequestBody User user) throws Exception {
		String tempass=this.encrypt(user.getPassword());
		user.setPassword(tempass);
		return service.saveUser(user);
	}

}
