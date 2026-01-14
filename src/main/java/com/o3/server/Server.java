package com.o3.server;

import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.concurrent.Executors;

import static com.o3.server.Util.sendResponse;


public class Server implements HttpHandler {

	private static final int port = 8001;
	private static final String databaseFile = "messages.db";

	private Server() {
	}

	@Override
	public void handle(HttpExchange exchange) {
		try {
			Headers headers = exchange.getRequestHeaders();
			System.out.println(Util.getContentType(headers));
			String method = exchange.getRequestMethod().toUpperCase();
			switch (method) {
				case "POST":
					postHandler(exchange);
					break;
				case "GET":
					getHandler(exchange);
					break;
				default:
					Util.notSupported(exchange);
					break;
			}
		} catch (Exception e) {
			System.err.println("Unhandled server error: " + e.getMessage());
			sendResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
		}
	}

	private void getHandler(HttpExchange exchange) {
		sendResponse(exchange, HttpURLConnection.HTTP_OK);
	}

	private void postHandler(HttpExchange exchange) {
		sendResponse(exchange, HttpURLConnection.HTTP_OK);
	}

	/**
	 * Creates an SSLContext for the server using the provided keystore and password.
	 *
	 * @param keystore The path to the keystore file.
	 * @param passwd   The password for the keystore.
	 * @return An initialized SSLContext configured with the keystore.
	 * @throws Exception If an error occurs while loading the keystore or initializing the SSLContext.
	 */
	private static SSLContext myServerSSLContext(String keystore, String passwd) throws Exception {
		char[] passphrase = passwd.toCharArray();
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(keystore), passphrase);

		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, passphrase);

		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ks);

		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		return ssl;
	}


	/**
	 * Configures the SSLContext for the provided HTTPS server.
	 * If no keystore path and password are provided, defaults are used.
	 *
	 * @param server The HTTPS server to configure.
	 * @param args   Command-line arguments containing the keystore path and password (optional).
	 * @throws Exception If an error occurs while setting up the SSLContext.
	 */
	private static void SSLContextSetup(HttpsServer server, String[] args) throws Exception{
		SSLContext sslContext;
		if (args.length < 2) {
			System.out.println("Using default keystore path and password");
			System.out.println("For different keystore use: \"java Server <keystorePath> <keystorePassword>\"");
			sslContext = myServerSSLContext("keystore.jks", "makkarakeitto");
		} else {
			sslContext = myServerSSLContext(args[0], args[1]);
		}
		//set up the HTTPS Configuration
		server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
			@Override
			public void configure (HttpsParameters params) {
				params.getClientAddress();
				SSLContext c = getSSLContext();
				SSLParameters sslParams = c.getDefaultSSLParameters();
				params.setSSLParameters(sslParams);
			}
		});
	}

	/**
	 * Creates and configures an HTTP context for the provided HTTPS server.
	 *
	 * @param server The HTTPS server to configure.
	 * @param path   The path for the HTTP context.
	 * @param auth   The authenticator to use for the context.
	 * @param type   The type of context to create (e.g., "REGISTRATION", "DATA", "SEARCH", "TEST").
	 * @param ws     The WeatherService instance (used for "DATA" context).
	 */
	private static void createContext(HttpsServer server,
									  String path,
									  UserAuthenticator auth,
									  String type, WeatherService ws) throws SQLException, IOException {
		HttpContext context;
		type = type.toUpperCase();
		switch (type) {
			case "REGISTRATION":
				server.createContext(path, new RegistrationHandler(auth));
				break;
			case "DATA":
				context = server.createContext(path, new ObservationHandler(databaseFile, ws));
				context.setAuthenticator(auth);
				break;
			case "SEARCH":
				context = server.createContext(path, new SearchHandler(databaseFile));
				context.setAuthenticator(auth);
				break;
			case "TEST":
				server.createContext(path, new Server());
				break;
			default:
				context = server.createContext(path, new Server());
				context.setAuthenticator(auth);
				break;
		}
	}

	public static void main(String[] args) {
		try {
			// Create the https server to port 8001 with default logger
			HttpsServer server = HttpsServer.create(new InetSocketAddress(port),0);

			// Initialize database
			MessageDataBase.getInstance(databaseFile);

			// Create client for communication with weather service
			WeatherService weatherService = new WeatherService();

			// Create authenticator
			UserAuthenticator authenticator = new UserAuthenticator(databaseFile);

			// Create contexts
			createContext(server, "/datarecord", authenticator, "data", weatherService);
			createContext(server, "/registration", authenticator, "registration", weatherService);
			createContext(server, "/search", authenticator, "search", weatherService);

			// Setup and configure SSLContext
			SSLContextSetup(server, args);

			// Set server to use multithreading
			server.setExecutor(Executors.newCachedThreadPool());

			server.start();
			System.out.println("Server started on port: " + port);

		} catch (IOException e) {
			System.err.println("[SERVER START FAIL] File error: " + e.getMessage());
		} catch (SQLException e) {
			System.err.println("[SERVER START FAIL] SQL Database creation error: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("[SERVER START FAIL] Unhandled error: " + e.getMessage());
		}
	}
}
