package Server;

import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class User {

  /**
   * Classe User: oggetto contenuto all'interno dell'attachment, contiene le informazioni
   * relative al client collegato, il suo SocketChannel e DatagramChannel, una
   * lock online per modificare la variabile booleana isOnline, consente di sapere se un 
   * utente è online o meno. Una lock sfida per modificare la variabile booleana
   * iSfida per sapere se un client è impegnato in una sfida, e infine l'username del client
   * e la porta UDP del datagramChannel
   */
  private SocketChannel socketChannel;
  private DatagramChannel datagramChannel;
  private Lock online = new ReentrantLock();
  private boolean isOnline;
  private boolean isSfida;
  private Lock sfida = new ReentrantLock();
  public String username;
  public int udpPort;

  public User(SocketChannel channel) {
    this.socketChannel = channel;
    this.isOnline = false;
    this.isSfida = false;
  }

  /**
   * Consente di sapere se un utente è online o meno
   * 
   * @return isOnline
   */
  public boolean isOnline() {
    online.lock();
    try {
      return isOnline;
    }
    finally {
      online.unlock();
    }
  }

  /**
   * Consente di settare un utente a online una volta 
   * effettuato il login
   * 
   * @return true se ha avuto successo, false se era gia online
   */
  public boolean setOnline() {
    online.lock();
    try {
      if (!this.isOnline) {
        this.isOnline = true;
        return true;
      }
      return false;
    }
    finally {
      online.unlock();
    }
  }

  /**
   * Consente di settare un utente offline in fase di logout
   * 
   * @return true se ha avuto successo, false se era gia offline
   */
  public boolean setOffline() {
    online.lock();
    try {
      if (this.isOnline) {
        this.isOnline = false;
        return true;
      }
      return false;
    }
    finally {
      online.unlock();
    }
  }

  /**
   * Consente di settare un utente in sfida
   * 
   * @return true se ha avuto successo, false se era gia impegnato in sfida
   */
  public boolean setSfidaTrue() {
    sfida.lock();
    try {
      if (!this.isSfida) {
        this.isSfida = true;
        return true;
      }
      return false;
    }
    finally {
      sfida.unlock();
    }
  }

  /**
   * Consente di rimuovere un utente dalla condizione di sfida
   * 
   * @return true se ha avuto successo, false se non era impegnato in una sfida
   */
  public boolean setSfidaFalse() {
    sfida.lock();
    try {
      if (this.isSfida) {
        this.isSfida = false;
        return true;
      }
      return false;
    }
    finally {
      sfida.unlock();
    }
  }

  /**
   * Consente di sapere se un utente è impegnato in una sfida o meno
   * 
   * @return true se l'utente è in condizione di sfida, false altrimenti
   */
  public boolean isSfida() {
    sfida.lock();
    try {
      return isSfida;
    }
    finally {
      sfida.unlock();
    }
  }

  public void setSocketChannel(SocketChannel channel) {
    this.socketChannel = channel;
  }

  public SocketChannel getSocketChannel() {
    return this.socketChannel;
  }

  public void setDatagramChannel(DatagramChannel channel) {
    this.datagramChannel = channel;
  }

  public DatagramChannel getDatagramChannel() {
    return this.datagramChannel;
  }

}