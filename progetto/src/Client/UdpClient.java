package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import javax.swing.JFrame;

import Server.Registration;

public class UdpClient implements Runnable {

  /**
   * Classe UdpClient: thread sempre attivo che riceve pacchetti Udp dal server
   * Quando viene mandata una richiesta di sfida, la receive si sblocca e 
   * e viene chiamato il metodo sullo stub RMI notificaClient.
   * Se l'utente accetta la sfida allora risponde al server con 1 e aggiunge 
   * alla coda condivisa un valore, se rifiuta la sfida allora risponde al server con 0,
   * se è gia impegnato in una sfida allora risponde al server con 2
   */
  ClientManager client;
  DatagramSocket datagramSocket;
  Registration registration;
  String username;
  JFrame frame;

  public UdpClient(ClientManager client,DatagramSocket datagramSocket,Registration registration,String username,JFrame jFrame) {
    this.client = client;
    this.datagramSocket = datagramSocket;
    this.registration = registration;
    this.username = username;
    this.frame = jFrame;
  }

  @Override
  public void run() {
    while (true) {

      try {
        byte[] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer, 100);
        datagramSocket.receive(packet);
        String request = new String(packet.getData());
        request = "Ciao " + username + " hai una nuova notifica!\n" + request;
        int portResponse = packet.getPort();
        byte[] buf = new byte[1];
        if (client.isSfida.get() == true) {
          buf[0] = 2;
        }
        else {
          int res = registration.notificaClient(request,frame);
          if (res == 1) {
            buf[0] = 1;
            client.queue.add(1);
          }
          else buf[0] = 0;  
        }
        DatagramPacket toSnd = new DatagramPacket(buf,buf.length,datagramSocket.getLocalAddress(),portResponse);
        datagramSocket.send(toSnd);
      } catch (SocketException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
      
  }  

}