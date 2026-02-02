package com.example.Ecomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthRequestDTO {
    
	  @JsonProperty("identifier") 
	private String username; 

	private String password;

	public String getUsername() { 
		return username;
	}

	public void setUsername(String username) { 
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public AuthRequestDTO() {
	}

	public AuthRequestDTO(String username, String password) { 
		this.username = username;
		this.password = password;
	}

}
