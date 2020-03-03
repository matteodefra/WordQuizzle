package Client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import Server.Registration;
import Shared.Connection;
import Shared.Message;

public class ClientManager {


  /**
   * Classe ClientManager: classe che si occupa della logica di funzionamento del client
   * Contiene una coda condivisa, usata al momento della sfida, il SocketChannel e 
   * DatagramChannel del client, gli oggetti Remote e Registration per accedere 
   * allo stub RMI, una variabile AtomicBoolean per impostare un client in fase di sfida
   * e il nome dell'username settato al momento del login
   */
  public LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

  public SocketChannel socket;
  public Registration registration;
  Remote remote;
  int udpPort;
  public String username;
  public AtomicBoolean isSfida = new AtomicBoolean(false);
  public DatagramSocket datagramSocket;


  public ClientManager() throws IOException {

    InetSocketAddress address = new InetSocketAddress("localhost", 6789);
    this.socket = SocketChannel.open(address);
    this.socket.configureBlocking(false);
    int rmiPort = 1919;
    try {
      Registry r = LocateRegistry.getRegistry(rmiPort);
      remote = r.lookup("REGISTRATION-SERVICE");
      registration = (Registration) remote;
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    try {
      datagramSocket = new DatagramSocket();
      int port = datagramSocket.getLocalPort();
      this.udpPort = port;
      
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }
    try { 
      sendPortNumber();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }

  /**
   * Consente a un client di effettuare il login
   * 
   * @param username nome utente
   * @param password password utente
   * @return il codice del Messaggio di risposta
   * @throws IOException
   */
  public int login(String username, String password) throws IOException {
    if (username == null || password == null) {
      throw new NullPointerException();
    }
    Message sndMessage = new Message(Message.LOGIN_CODE, "login " + username + " " + password);
    Connection.write(socket, sndMessage);
    Message rspMessage = Connection.read(socket);
    if (rspMessage.getType() == Message.LOGIN_OK) {
      this.username = username;
      return rspMessage.getType();
    }
    else return rspMessage.getType();
  }

  /**
   * Consente a un client di registrarsi al servizio
   * 
   * @param username nome utente
   * @param password password utente
   * @return 1 se la registrazione ha avuto successo 0 altrimenti
   * @throws RemoteException
   */
  public int register(String username, String password) throws RemoteException {
    if (username == null || password == null) throw new NullPointerException();

    int res = registration.registra_utente(username,password);
    if (res == 1) return 1;
    else return 0;
  }

  /**
   * Usata in fase di connessione per comunicare al server la porta UDP del client
   * 
   * @throws IOException
   */
  public void sendPortNumber() throws IOException {
    Connection.write(socket,new Message(Message.REQUEST_SOCKET,this.udpPort));
    Connection.read(socket);
  }

  /**
   * Consente di stringere un'amicizia con un altro utente
   * 
   * @param name nome amico da aggiungere
   * @return il codice del messaggio di risposta
   * @throws IOException
   */
  public int addFriend(String name) throws IOException {
    if (name == null) throw new NullPointerException();

    Connection.write(socket,new Message(Message.ADD_FRIEND_CODE,"aggiungi_amico " + this.username + " " + name));
    Message rspMessage = Connection.read(socket);
    return rspMessage.getType();
  }

  /**
   * Mostra gli amici dell'utente connesso
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message showFriends() throws IOException {
    Connection.write(socket,new Message(Message.FRIENDS_LIST_CODE,"lista_amici " + this.username));
    Message rspMessage = Connection.read(socket);
    return rspMessage;
  }

  /**
   * Mostra il punteggio dell'utente connesso
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message showScore() throws IOException {
    Connection.write(socket,new Message(Message.SHOW_POINTS_CODE,"mostra_punteggio " + this.username));
    Message rspMessage = Connection.read(socket);
    return rspMessage;
  }

  /**
   * Mostra la classifica dell'utente e i suoi amici
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message showScoreBoard() throws IOException{
    Connection.write(socket,new Message(Message.SHOW_SCORE_CODE,"mostra_classifica " + this.username));
    Message rspMessage = Connection.read(socket);
    return rspMessage;
  }

  /**
   * Consente di effettuare il logout
   * 
   * @return il codice del messaggio di risposta
   * @throws IOException
   */
  public int logout() throws IOException{
    Connection.write(socket,new Message(Message.LOGOUT_CODE,"logout " + this.username));
    Message rspMessage = Connection.read(socket);
    return rspMessage.getType();
  }

  /**
   * Consente di sfidare un altro utente 
   * 
   * @param name nome dell'utente da sfidare
   * @return il codice del messaggio di risposta
   * @throws IOException
   */
  public int sfida(String name) throws IOException {
    Connection.write(socket, new Message(Message.SFIDA_CODE,"sfida " + this.username + " " + name));
    Message rspMessage = Connection.read(socket);
    return rspMessage.getType();
  }

  /**
   * Manda la parola tradotta e riceve la prossima parola da tradurre
   * 
   * @param word parola tradotta dall'utente
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message sendReceive(String word) throws IOException {
    Connection.write(socket,new Message(Message.SFIDA_DURING,word));
    Message toTranslate = Connection.read(socket);
    return toTranslate;
  }

  /**
   * Consente di ricevere la prima parola
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message receiveWord() throws IOException {
    return Connection.read(socket);
  }

  /**
   * Consente all'utente che finisce per primo la sfida di attendere il suo avversario
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message waitFriend() throws IOException {
    Connection.write(socket,new Message(Message.SFIDA_WAIT_FRIEND,"In attesa"));
    return Connection.read(socket);
  }

  /**
   * Consente all'utente di mandare un messaggio non valido in attesa
   * della preparazione della sfida
   * 
   * @return il messaggio di risposta
   * @throws IOException
   */
  public Message reqNotValid() throws IOException {
    Connection.write(socket,new Message(Message.REQUEST_NOT_VALID,"Sfida try"));
    return Connection.read(socket);
  }

}
