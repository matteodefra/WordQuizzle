package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.Callable;


public class UdpRequest implements Callable<Integer> {

  /**
   * Classe UdpRequest: classe che implementa Callable, viene lanciato 
   * nel momento di una richiesta di sfida per mandare la richiesta UDP
   * al client che viene sfidato, prende una HashMap per trovare l'utente 
   * a cui mandare la richiesta e prendere il relativo valore, la porta UDP
   * a cui mandare il pacchetto, e lo stato del client collegato
   */
  public HashMap<String, Integer> users;
  public ClientState clientState;

  public UdpRequest(HashMap<String,Integer> users, ClientState state) {
    this.users = users;
    this.clientState = state;
  }

  @Override
  public Integer call() throws Exception {
    try {
      /**
       * Viene mandato un pacchetto con le informazioni della sfida,
       * il server aspetta massimo 10 secondi la risposta dall'amico, se non 
       * arriva risposta entro questo tempo la sfida viene annullata. Se invece
       * arriva una risposta, se la risposta è di tipo 1 allora la sfida è stata accettata,
       * se la risposta è 0 allora la sfida è stata rifiutata, mentre se 
       * la risposta è 2 allora lo sfidato è gia impegnato in una sfida
       */
      int port = users.get(clientState.usernameSfida);
      DatagramSocket datagramChannel = new DatagramSocket();
      String sfida = "Ciao sono " + clientState.user.username + " voglio sfidarti!";
      byte[] toSnd = new byte[100];
      toSnd = sfida.getBytes();
      DatagramPacket packet = new DatagramPacket(toSnd,toSnd.length,datagramChannel.getLocalAddress(),port);
      datagramChannel.setSoTimeout(10000);
      datagramChannel.send(packet);
      byte[] resp = new byte[1];
      DatagramPacket receive = new DatagramPacket(resp,1);
      datagramChannel.receive(receive);
      if (resp[0] == 1) {
        datagramChannel.close();
        return 1;
      }
      else if (resp[0] == 0){
        datagramChannel.close();
        return 0;
      }
      else {
        datagramChannel.close();
        return 2;
      }
    } catch (SocketTimeoutException e1) {
      return 0; 
    }
    catch (IOException e) {
      return 0;
    } 
    catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }



}