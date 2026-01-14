package com.o3.server;

import java.io.IOException;
import java.sql.SQLException;
import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends BasicAuthenticator {

	private final MessageDataBase database;

	public UserAuthenticator(String dbName) throws SQLException, IOException {
		super("datarecord");
		database = MessageDataBase.getInstance(dbName);
	}

	@Override
	public boolean checkCredentials(String username, String password) {
		try {
			return database.authenticateUser(username, password);
		} catch (SQLException e) {
			System.err.println("Database error during authentication: " + e.getMessage());
			return false;
		}
	}

	public boolean isUser(String username) {
		try {
			return database.isUser(username);
		} catch (SQLException e) {
			System.err.println("Database error during username check: " + e.getMessage());
			return false;
		}
	}

	public boolean emailInUse(String email) {
		try {
			return database.usedEmail(email);
		} catch (SQLException e) {
			System.err.println("Database error during email check: " + e.getMessage());
			return false;
		}
	}

	public boolean invalidString(String str) {
		return str == null || str.isEmpty();
	}

	public boolean invalidEmail(String email) {
		return email == null || email.length() < 3 || !email.contains("@");
	}

	public void addUser(User newUser) throws SQLException {
		database.insertUser(newUser);
	}
}
