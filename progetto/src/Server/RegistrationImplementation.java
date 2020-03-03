package Server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;


public class RegistrationImplementation extends UnicastRemoteObject implements Registration, Serializable {

  /**
   * Classe RegistrationImplementation: implementa le funzioni definite nell'interfaccia
   * Registration, sono le funzioni che verranno caricate come stub RMI.
   * Contiene un Hashtable registeredUsers per controllare piu velocemente al momento della 
   * registrazione se quell'utente è gia registrato o no, e un ArrayList info che contiene
   * i dati degli utenti
   */
  private static final long serialVersionUID = 1L;
  Hashtable<String, String> registeredUsers;
  ArrayList<InfoServer> info = new ArrayList<>();

  /**
   * Funzione per recuperare il contenuto del Json
   */
  public void recoverFromJson() {
    registeredUsers = new Hashtable<>();
    try {
      WordQuizzle.writeGsonLock.lock();
      File file = new File("infoserver.json");
      if (!file.exists()) {
        file.createNewFile();
      }
      FileReader reader = new FileReader(file);
      if (Files.size(Paths.get("infoserver.json")) == 0) {
        reader.close();
        WordQuizzle.writeGsonLock.unlock();
        return;
      }
      Gson gson = new Gson();
      java.lang.reflect.Type collectionType = new TypeToken<ArrayList<InfoServer>>() {
      }.getType();
      info = gson.fromJson(reader, collectionType);
      WordQuizzle.writeGsonLock.unlock();
      for (InfoServer s : info) {
        registeredUsers.put(s.userName, s.password);
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public RegistrationImplementation() throws RemoteException {
    recoverFromJson();
  }

  /**
   * Gestione della registrazione di un utente. Consente a un utente di registrarsi al servizio.
   * La funzione recoverFromJson chiamata all'inizio legge il contenuto del file Json per 
   * vedere se l'utente è gia registrato.
   * 
   * @param userNick nome dell'utente
   * @param userPass password dell'utente
   * 
   * @throws RemoteException errore registrazione RMI
   */
  @Override
  public int registra_utente(String userNick, String userPass) throws RemoteException {
    if (registeredUsers.containsKey(userNick)) {
      return -1;
    } else {
      registeredUsers.put(userNick, userPass);
      info.add(new InfoServer(userNick, userPass, new ArrayList<String>(), 0));
      try {
        WordQuizzle.writeGsonLock.lock();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String toJson = gson.toJson(info);
        FileWriter writer = new FileWriter("infoserver.json");
        writer.write(toJson);
        writer.close();
        WordQuizzle.writeGsonLock.unlock();
      } catch (IOException e) {
        WordQuizzle.writeGsonLock.unlock();
        e.printStackTrace();
      } catch (JsonIOException e1) {
        e1.printStackTrace();
      } catch (Exception e2) {
        WordQuizzle.writeGsonLock.unlock();
        e2.printStackTrace();
      }

      return 1;
    }
  }

  /**
   * Gestione della notifica. Quando un client viene sfidato da un altro manda notifica 
   * tramite RMI di una sfida in attesa
   * 
   * @param request Richiesta dello sfidante
   * @return 1 se la richiesta è stata accettata 0 altrimenti
   * @throws RemoteException errore di notifica al Client
   */
  @Override
  public int notificaClient(String request,JFrame frame) throws RemoteException {
    int accepted = JOptionPane.showConfirmDialog(frame,request, "Nuova sfida", JOptionPane.YES_NO_OPTION);
    if (accepted == JOptionPane.YES_OPTION) {
      return 1;
    }
    else return 0;
  }

}