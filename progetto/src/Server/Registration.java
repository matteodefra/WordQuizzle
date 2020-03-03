package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.JFrame;

public interface Registration extends Remote {

  /**
   * Funzione per registrare un utente al servizio
   * 
   * @param userNick utente da registrare
   * @param userPass password utente
   * @return 1 se la registrazione ha avuto successo, -1 altrimenti
   * @throws RemoteException
   */
  public int registra_utente(String userNick,String userPass) throws RemoteException;

  /**
   * Funzione per notificare un client di una richiesta di sfida
   * 
   * @param request richiesta di sfida
   * @return 1 se la richiesta è stata accettata 0 altrimenti
   * @throws RemoteException
   */
  public int notificaClient(String request,JFrame frame) throws RemoteException;

}