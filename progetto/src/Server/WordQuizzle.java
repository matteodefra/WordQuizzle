package Server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import Shared.Connection;
import Shared.Message;
import Shared.ValueHashmap;
import Shared.ValueOne;

public class WordQuizzle {

  /**
   * Classe WordQuizzle: Server principale, contiene il selector e vari oggetti.
   * ArrayList dict: contiene il dizionario di parole italiane
   * ReentrantLock writeGsonLock: lucchetto da usare al momento della scrittura
   * o lettura sul file Json per evitare problemi di concorrenza
   * HashMap users: hashmap per tenere conto degli utenti online, contiene come 
   * chiave il nome dell'utente e come valore un intero che indica il numero di 
   * porta UDP del datagramSocket dove effettuare l'eventuale richiesta di sfida
   * ConcurrentHashMap fighting: hashMap usata in fase di sfida, qui vengono aggiunti
   * come chiave il nome dello sfidato, i punti effettuati in questa sfida, le parole 
   * da tradurre e le parole tradotte e due booleani per sincronizzare la fine della sfida
   * tra i due utenti 
   */
  private static ServerSocketChannel serverSocket;
  private static Selector selector;
  private static InetSocketAddress inetAddress;
  public static ArrayList<String> dict = new ArrayList<>();
  public static ReentrantLock writeGsonLock = new ReentrantLock();

  // Utenti online
  static HashMap<String, Integer> users = new HashMap<>();

  // Utenti in sfida
  static ConcurrentHashMap<ValueOne,ValueHashmap> fighting = new ConcurrentHashMap<>();

  public WordQuizzle() {

  }

  public static void main(String[] args) throws FileNotFoundException {
    int rmiPort = 1919;

    /**
     * Inizializzazione dello stub RMI
     */
    try {
      RegistrationImplementation registration = new RegistrationImplementation();
      Registration register = (Registration) registration;

      LocateRegistry.createRegistry(rmiPort);
      Registry registry = LocateRegistry.getRegistry(rmiPort);

      registry.rebind("REGISTRATION-SERVICE", register);

    } catch (RemoteException e) {
      e.printStackTrace();
      return;
    }

    /**
     * Lettura dal dizionario di tutte le parole e salvataggio nell'ArrayList dict
     */
    try {
      BufferedReader reader = new BufferedReader(new FileReader("dizionario.txt"));
      String str;
      while ((str = reader.readLine()) != null) {
        dict.add(str);
      }
      reader.close();
    } catch (FileNotFoundException e) {

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    int serverPort = 6789;

    /**
     * ThreadPool per gestire le richieste in arrivo
     */
    ExecutorService executorService = Executors.newFixedThreadPool(20);

    /**
     * Inizializzazione del ServerSocketChannel per accettare connessioni in entrata
     */
    try {
      serverSocket = ServerSocketChannel.open();
      ServerSocket socket = serverSocket.socket();
      inetAddress = new InetSocketAddress(serverPort);
      socket.bind(inetAddress);
      serverSocket.configureBlocking(false);
      selector = Selector.open();

      serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    while (true) {
      try {
        selector.select();
      } catch (IOException e) {
        e.printStackTrace(); 
        break; 
      }
      Set <SelectionKey> readyKeys = selector.selectedKeys();
      Iterator <SelectionKey> iterator = readyKeys.iterator();
    
      while (iterator.hasNext()) {
        
        SelectionKey key = iterator.next();
        iterator.remove();
        
        try {
          /**
           * Caso accetable: accept di una nuova connessione e registrazione 
           * di un client in operazione di lettura, creazione dell'oggetto 
           * ClientState che verrá usato come attachment
           */
          if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel(); 
            SocketChannel client = server.accept();
            // Connessione accettata 
            User user = new User(client);
            ClientState state = new ClientState(user);
            state.user.setSocketChannel(client);
            System.out.println(client); 
            client.configureBlocking(false); 
            client.register(selector, SelectionKey.OP_READ,state);
          }
          /**
           * Caso readable: viene recuperato l'attachment, viene letta la richiesta
           * e la richiesta del client viene gestita da un thread del ThreadPool
           */
          else if (key.isReadable()) {
            ClientState state = (ClientState) key.attachment();
            try {
              state.requestMessage = Connection.read((SocketChannel) key.channel());
              key.interestOps(0);
              WorkerThread worker = new WorkerThread(selector,key,state);
              executorService.submit(worker);  
            } catch (IOException e) {
              if (state.user.isSfida() == true) {
                ValueOne one = new ValueOne(state.user.username);
                ValueHashmap two = new ValueHashmap(state.user.username);
                WordQuizzle.fighting.forEach((arg0, arg1) -> {
                  if (arg0.equals(one)) {
                    if (arg1.hasInterrupted == true) {
                      fighting.remove(one);
                    }
                    else {
                      arg1.hasFinishedFirst = true;
                      arg1.hasInterruptedFirst = true;
                      arg0.sfidaPoint = -9;
                    }
                  } else if (arg1.equals(two)) {
                    if (arg1.hasInterruptedFirst == true) {
                      fighting.remove(one);
                    }
                    else {
                      arg1.hasFinished = true;
                      arg1.hasInterrupted = true;
                      arg1.pointsSfida = -9;     
                    }
                  }
                });
              }
              users.remove(state.user.username);
              key.cancel();
              selector.wakeup();
            }
            
          }
          /**
           * Caso writable: viene recuperato l'attachment, se il client si trova in fase di sfida
           * se ancora non sono esaurite le parole viene mandata la prossima parola,
           * altrimenti manda il messaggio di eventuale fine. Se invece non si trova in fase di sfida
           * allora viene mandata la risposta generata nel thread
           */
          else if (key.isWritable()) {
            SocketChannel client = (SocketChannel) key.channel();
            ClientState state = (ClientState) key.attachment();
            if (state.user.isSfida()) {
              if (state.responseMessage.getType() == Message.SFIDA_ACCETABLE) {
                try {
                  Connection.write(client,state.responseMessage);
                  state.responseMessage = new Message(Message.SFIDA_DURING,"Inizio sfida");
                  client.register(selector,SelectionKey.OP_WRITE,state);    
                } catch (IOException e) {
                  if (state.user.isSfida() == true) {
                    ValueOne one = new ValueOne(state.user.username);
                    ValueHashmap two = new ValueHashmap(state.user.username);
                    WordQuizzle.fighting.forEach((arg0, arg1) -> {
                      if (arg0.equals(one)) {
                        if (arg1.hasInterrupted == true) {
                          fighting.remove(one);
                        }
                        else {
                          arg1.hasFinishedFirst = true;
                          arg1.hasInterruptedFirst = true;
                          arg0.sfidaPoint = -9;
                        }
                      } else if (arg1.equals(two)) {
                        if (arg1.hasInterruptedFirst == true) {
                          fighting.remove(one);
                        }
                        else {
                          arg1.hasFinished = true;
                          arg1.hasInterrupted = true;
                          arg1.pointsSfida = -9;     
                        }
                      }
                    });
                  }
                  users.remove(state.user.username);
                  key.cancel();
                  selector.wakeup();
                }
              }
              else {
                try { 
                  String toTranslate;
                  if (state.iterWords == 9) {
                    Connection.write(client,state.responseMessage);
                    client.register(selector,SelectionKey.OP_READ,state);
                  }  
                  else if (state.iterWords < 8) {
                    toTranslate = state.words.get(state.iterWords);
                    state.responseMessage = new Message(Message.SFIDA_DURING,"Traduci " + toTranslate);
                    Connection.write(client,state.responseMessage);
                    client.register(selector,SelectionKey.OP_READ,state);
                  }
                  else {    
                    Connection.write(client,state.responseMessage);
                    client.register(selector,SelectionKey.OP_READ,state);                   
                  }
                }
                catch(IOException e) {
                  if (state.user.isSfida() == true) {
                    ValueOne one = new ValueOne(state.user.username);
                    ValueHashmap two = new ValueHashmap(state.user.username);
                    WordQuizzle.fighting.forEach((arg0, arg1) -> {
                      if (arg0.equals(one)) {
                        if (arg1.hasInterrupted == true) {
                          fighting.remove(one);
                        }
                        else {
                          arg1.hasFinishedFirst = true;
                          arg1.hasInterruptedFirst = true;
                          arg0.sfidaPoint = -9;
                        }
                      } else if (arg1.equals(two)) {
                        if (arg1.hasInterruptedFirst == true) {
                          fighting.remove(one);
                        }
                        else {
                          arg1.hasFinished = true;
                          arg1.hasInterrupted = true;
                          arg1.pointsSfida = -9;     
                        }
                      }
                    });
                  }
                  users.remove(state.user.username);
                  key.cancel();
                  selector.wakeup();
                }
      
              }
              
            }
            else {
              try {
                Connection.write(client,state.responseMessage);
                client.register(selector,SelectionKey.OP_READ,state);
              } catch (IOException e) {
                if (state.user.isSfida() == true) {
                  ValueOne one = new ValueOne(state.user.username);
                  ValueHashmap two = new ValueHashmap(state.user.username);
                  WordQuizzle.fighting.forEach((arg0, arg1) -> {
                    if (arg0.equals(one)) {
                      if (arg1.hasInterrupted == true) {
                        fighting.remove(one);
                      }
                      else {
                        arg1.hasFinishedFirst = true;
                        arg1.hasInterruptedFirst = true;
                        arg0.sfidaPoint = -9;
                      }
                    } else if (arg1.equals(two)) {
                      if (arg1.hasInterruptedFirst == true) {
                        fighting.remove(one);
                      }
                      else {
                        arg1.hasFinished = true;
                        arg1.hasInterrupted = true;
                        arg1.pointsSfida = -9;     
                      }
                    }
                  });
                }
                users.remove(state.user.username);
                key.cancel();
                selector.wakeup();
              }
            }
          }            
        } catch (Exception e) {
          key.cancel();
          e.printStackTrace();
        }

      }
    }
  }

}