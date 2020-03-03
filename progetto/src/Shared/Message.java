package Shared;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class Message implements Serializable {

  /**
	 * Classe Message: implementa l'interfaccia Serializable, viene usata per la comunicazione 
	 * client-server. Ogni oggetto scambiato contiene un tipo e un messaggio, il tipo
	 * identifica la richiesta del client e la risposta del server e il messaggio 
	 * è la risposta mandata del server
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int LOGIN_CODE = 0;
	public static final int LOGIN_OK = 1;
	public static final int LOGIN_ERR = 2;
	public static final int LOGIN_ERROR_PASSWORD = 3;
	public static final int LOGIN_USER_ALREADY_ONLINE = 4;
	public static final int LOGOUT_CODE = 5;
	public static final int LOGOUT_OK = 6;
	public static final int LOGOUT_ERR = 7;
	public static final int ADD_FRIEND_CODE = 8;
	public static final int ADD_FRIEND_OK = 9;
	public static final int ADD_FRIEND_ERR = 10;
	public static final int ADD_FRIEND_ALREADY_FRIEND = 11;
	public static final int ADD_FRIEND_NOT_REGISTERED = 12;
	public static final int FRIENDS_LIST_CODE = 13;
	public static final int FRIENDS_LIST_OK = 14;
	public static final int SHOW_POINTS_CODE = 15;
	public static final int SHOW_POINTS_OK = 16;
	public static final int SHOW_SCORE_CODE = 17;
	public static final int SHOW_SCORE_OK = 18;
	public static final int SFIDA_CODE = 19;
	public static final int SFIDA_ACCETABLE = 20;
	public static final int SFIDA_WAIT_FRIEND = 21;
	public static final int SFIDA_NOT_FRIEND = 22;
	public static final int SFIDA_ALREADY = 23;	
	public static final int SFIDA_FRIEND_NOT_ONLINE = 24;	
	public static final int SFIDA_DURING = 25;
	public static final int SFIDA_FINISHING = 26;
	public static final int SFIDA_REJECTED = 27;

	public static final int USER_NOT_REGISTERED = 28;
	public static final int USER_NOT_LOGGED = 29;

	public static final int REQUEST_NOT_VALID = 30;

	public static final int REQUEST_SOCKET = 31;
		
	public static final int SFIDA_CANNOT_CONNECT = 32;

	public static final int ERROR_SERVER = 33;

  int type;
  byte[] payload;

  public Message(int type, int payload) {
		if(type < 0) throw new IllegalArgumentException();
		this.type = type;
		
		// Conversione da int a byte[] usando un buffer
		ByteBuffer b = ByteBuffer.allocate(Integer.BYTES);
		b.putInt(payload);
		this.payload = b.array();
	}
	
	public Message(int type, String payload) {
		if(type < 0) throw new IllegalArgumentException();
		if(payload == null) throw new NullPointerException();
		
		this.type = type;
		this.payload = payload.getBytes();
	}
	
	public Message(int type, byte[] payload) {
		if(type < 0) throw new IllegalArgumentException();
		if(payload == null) throw new NullPointerException();
		
		this.type = type;
		this.payload = payload;
	}


	public int getType() {
		return type;
	}
	
	public int getPayloadInt() {
		ByteBuffer b = ByteBuffer.wrap(payload);
		return b.getInt(0);	
	}

	public String getPayloadString() {
		String s = new String(payload);
		return s;
	}
	
	public byte[] getPayloadByte() {
		return payload;
	}

}