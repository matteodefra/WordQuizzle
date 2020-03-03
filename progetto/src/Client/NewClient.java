package Client;

import java.io.IOException;

import javax.swing.JOptionPane;

import Client.GUI.StartForm;

public class NewClient {

  /**
   * Classe NewClient: crea un'istanza di ClientManager, logica di funzionamento del client
   * Se il server è offline viene mostrato un messaggio e viene terminato, altrimenti 
   * viene lanciata la StartForm per il login o registrazione
   */
  @SuppressWarnings("unused")
  public static void main(String[] args) {

    ClientManager client;
    try {
      client = new ClientManager();
      StartForm initialForm = new StartForm(client);
    } catch (IOException e) {
      JOptionPane.showMessageDialog(null,"Server disconnesso", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    
  }
}