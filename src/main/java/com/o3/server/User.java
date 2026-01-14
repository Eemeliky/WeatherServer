package com.o3.server;

public class User {

	private final String username;
	private final String password;
	private final String email;
	private final String nickname;

	public User(String username, String password, String email) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.nickname = username;
	}

	public User(String username, String password, String email, String nickname) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.nickname = nickname;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public String getNickname() {
		return nickname;
	}

	public String getPassword() {
		return password;
	}
}
