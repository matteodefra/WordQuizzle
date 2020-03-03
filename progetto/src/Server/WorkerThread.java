package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

import Shared.GetJson;
import Shared.Message;
import Shared.NamePoint;
import Shared.ValueHashmap;
import Shared.ValueOne;

public class WorkerThread implements WordQuizzleInterface, Runnable {

  /**
   * Thread worker: si occupa di gestire la richiesta del client e creare la risposta
   * Utilizza il selector che gli viene passato insieme alla SelectionKey del client, 
   * l'attachment. Legge dal file infoserver.json il contenuto ad ogni inizio e salva 
   * le informazioni nell'ArrayList info, utilizza una ThreadPool di un solo thread per 
   * lanciare un Callable per la richiesta di sfida
   */
  public Selector selector;
  public SelectionKey key;
  public ClientState clientState;
  public ArrayList<InfoServer> info;
  public ExecutorService service = Executors.newFixedThreadPool(1);
  static ArrayList<String> sfida = new ArrayList<String>(8);

  public WorkerThread(Selector selector, SelectionKey key, ClientState clientState) {
    this.selector = selector;
    this.key = key;
    this.clientState = clientState;
    Gson gson = new Gson();
    File file =  new File("infoserver.json");
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException e) {
      }
    }
    FileReader reader;
    try {
      WordQuizzle.writeGsonLock.lock();
      reader = new FileReader(file);
      java.lang.reflect.Type collectionType = new TypeToken<ArrayList<InfoServer>>() {
      }.getType();
      this.info = gson.fromJson(reader, collectionType);
      WordQuizzle.writeGsonLock.unlock();
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    /**
     * Controllo se l'utente che sta facendo la richiesta sia stato sfidato da qualcuno
     */
    if (this.clientState.user.username == null) {
      try {
        String[] request = clientState.requestMessage.getPayloadString().split(" ");
        Message rspMessage;
  
        switch (this.clientState.requestMessage.getType()) {
          case Message.LOGIN_CODE:
            rspMessage = login(request[1], request[2]);
            break;
          case Message.LOGOUT_CODE:
            rspMessage = logout(request[1]);
            break;
          case Message.ADD_FRIEND_CODE:
            if (this.clientState.user.isOnline() == false) {
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = aggiungi_amico(request[1], request[2]);
            }
            break;
          case Message.FRIENDS_LIST_CODE:
            if (this.clientState.user.isOnline() == false) {
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = lista_amici(request[1]);
            }
            break;
          case Message.SHOW_POINTS_CODE:
            if (this.clientState.user.isOnline() == false) {
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = mostra_punteggio(request[1]);
            }
            break;
          case Message.SHOW_SCORE_CODE:
            if (this.clientState.user.isOnline() == false) {
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = mostra_classifica(request[1]);
            }
            break;
          case Message.SFIDA_CODE:
            if (clientState.user.isOnline() == false) {
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = sfida(request[1], request[2]);
            }
            break;
          case Message.SFIDA_WAIT_FRIEND:
            rspMessage = checkFinished();
            break;
          case Message.SFIDA_DURING:
            if (clientState.user.isSfida()) {
              rspMessage = sfida_during(clientState.requestMessage.getPayloadString());
            } else {
              rspMessage = new Message(Message.SFIDA_FINISHING, "finish");
            }
            break;
          case Message.REQUEST_SOCKET:
            rspMessage = savePortNumber();
            break;
          default:
            if (this.clientState.user.isOnline() == false) {  
              rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
            } else {
              rspMessage = request_not_valid();
            }
            break;
        }
          this.clientState.responseMessage = rspMessage;
          this.key.interestOps(4);
          this.selector.wakeup();      
      } finally {
      }
    }
    else {
      ValueHashmap value = new ValueHashmap(this.clientState.user.username);
      if (WordQuizzle.fighting.containsValue(value) && (this.clientState.user.isSfida() == false)) {
        boolean res = this.clientState.user.setSfidaTrue();
        if (res == true) {
          Iterator<Entry<ValueOne,ValueHashmap>> it = WordQuizzle.fighting.entrySet().iterator();
          while (it.hasNext()) {
            Entry<ValueOne,ValueHashmap> entry = it.next();
            if (entry.getValue().equals(value)) {
              if (entry.getKey().isInterrupted == true) {
                this.clientState.user.setSfidaFalse();
                this.clientState.responseMessage = new Message(Message.SFIDA_CANNOT_CONNECT, "Impossibile contattare il servizio");
                try {
                  this.key.channel().register(selector, SelectionKey.OP_WRITE, this.clientState);
                  this.selector.wakeup();
                  return;
                } catch (ClosedChannelException e) {
                  this.key.cancel();
                  this.selector.wakeup();
                  e.printStackTrace();
                  return;
                }
              }
              else {
                this.clientState.words.addAll(entry.getValue().sfidaWords);
                this.clientState.translatedWords.addAll(entry.getValue().translated);  
              }
            }
          }
          sfida.clear();
          this.clientState.iterWords = 0;
          this.clientState.correct.add(0);
          this.clientState.correct.add(0);
          this.clientState.timer = new Timer();
          this.clientState.timer.schedule(new TimerTask() {
            @Override
            public void run() {
              clientState.iterWords = 9;
            }
          }, 60000);
          this.clientState.responseMessage = new Message(Message.SFIDA_ACCETABLE, "Inizio sfida");
          try {
            this.key.channel().register(selector, SelectionKey.OP_WRITE, this.clientState);
            this.selector.wakeup();
            return;
          } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            this.key.cancel();
            this.selector.wakeup();
            e.printStackTrace();
            return;
          }
        }
      }
        try {
          String[] request = clientState.requestMessage.getPayloadString().split(" ");
          Message rspMessage;
          System.out.println(this.clientState.user.username + " " + this.clientState.requestMessage.getType());
    
          switch (this.clientState.requestMessage.getType()) {
            case Message.LOGIN_CODE:
              rspMessage = login(request[1], request[2]);
              break;
            case Message.LOGOUT_CODE:
              rspMessage = logout(request[1]);
              break;
            case Message.ADD_FRIEND_CODE:
              if (this.clientState.user.isOnline() == false) {
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = aggiungi_amico(request[1], request[2]);
              }
              break;
            case Message.FRIENDS_LIST_CODE:
              if (this.clientState.user.isOnline() == false) {
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = lista_amici(request[1]);
              }
              break;
            case Message.SHOW_POINTS_CODE:
              if (this.clientState.user.isOnline() == false) {
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = mostra_punteggio(request[1]);
              }
              break;
            case Message.SHOW_SCORE_CODE:
              if (this.clientState.user.isOnline() == false) {
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = mostra_classifica(request[1]);
              }
              break;
            case Message.SFIDA_CODE:
              if (clientState.user.isOnline() == false) {
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = sfida(request[1], request[2]);
              }
              break;
            case Message.SFIDA_WAIT_FRIEND:
              rspMessage = checkFinished();
              break;
            case Message.SFIDA_DURING:
              if (clientState.user.isSfida()) {
                rspMessage = sfida_during(clientState.requestMessage.getPayloadString());
              } else {
                rspMessage = new Message(Message.SFIDA_FINISHING, "finish");
              }
              break;
            case Message.REQUEST_SOCKET:
              rspMessage = savePortNumber();
              break;
            default:
              if (this.clientState.user.isOnline() == false) {  
                rspMessage = new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              } else {
                rspMessage = request_not_valid();
              }
              break;
          }
            this.clientState.responseMessage = rspMessage;
            this.key.interestOps(4);
            this.selector.wakeup();      
        } finally {
        }
      }
      }
        

/**
   * Consente all'utente che ha finito la sfida per primo di controllare periodicamente
   * se anche l'altro avversario ha terminato
   * 
   * @return un messaggio di continuare l'attesa se non ha terminato altrimenti
   *          il messaggio di resoconto della sfida
   */
  public Message checkFinished() {
    Message [] message = new Message[1];
    for (InfoServer arg0 : info) {
      if (arg0.userName.equals(this.clientState.user.username)) {
        for (Map.Entry<ValueOne, ValueHashmap> entry : WordQuizzle.fighting.entrySet()) {
          ValueOne key = entry.getKey();
          ValueHashmap value = entry.getValue();
          if (key.equals(new ValueOne(this.clientState.user.username))) {
            value.hasFinishedFirst = true;
            WordQuizzle.fighting.replace(key, entry.getValue(), value);
            if (value.hasFinished == true) {
              if (key.sfidaPoint > value.pointsSfida) {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + key.sfidaPoint + 3;
              } else if (key.sfidaPoint < value.pointsSfida) {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
              } else {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
              }
            } else
              return new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
          } else if (value.equals(new ValueHashmap(this.clientState.user.username))) {
            value.hasFinished = true;
            WordQuizzle.fighting.replace(entry.getKey(), entry.getValue(), value);
            if (value.hasFinishedFirst == true) {
              if (key.sfidaPoint > value.pointsSfida) {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
              } else if (key.sfidaPoint < value.pointsSfida) {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + value.pointsSfida + 3;
              } else {
                message[0] = new Message(Message.SFIDA_FINISHING,
                    "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                        + this.clientState.correct.get(0) + " risposte corrette e "
                        + this.clientState.correct.get(1) + " risposte sbagliate");
                arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
              }
            } else
              return new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
          }
        }
      }
    }
    this.clientState.usernameSfida = null;
    updateDB(this.clientState.user.username, this.clientState.sfidaPoint);
    this.clientState.sfidaPoint = 0;
    this.clientState.correct.clear();
    this.clientState.iterWords = 0;
    this.clientState.words.clear();
    this.clientState.translatedWords.clear();
    this.clientState.user.setSfidaFalse();
    ValueOne one = new ValueOne(this.clientState.user.username);
    ValueHashmap two = new ValueHashmap(this.clientState.user.username);
    Iterator<Entry<ValueOne,ValueHashmap>> it = WordQuizzle.fighting.entrySet().iterator();
    while (it.hasNext()) {
      Entry<ValueOne,ValueHashmap> entry = it.next();
      if (entry.getKey().equals(one)) {
        it.remove();
      }
      else if (entry.getValue().equals(two)) {
        it.remove();
      }
    }
    return message[0];
  }

  /**
   * Aggiorna il file infoserver.json con il punteggio effettuato 
   * da un utente
   * 
   * @param user nome utente della sfida
   * @param points punti effettuati dall'utente durante la sfida
   */
  private void updateDB(String user, int points) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    WordQuizzle.writeGsonLock.lock();
    FileReader reader;
    try {
      reader = new FileReader("infoserver.json");
      java.lang.reflect.Type collectionType = new TypeToken<ArrayList<InfoServer>>() {
      }.getType();
      this.info = gson.fromJson(reader, collectionType);
      this.info.forEach((arg0) -> {
        if (arg0.userName.equals(user)) {
          arg0.totalPoint = arg0.totalPoint + points;
        }
      });
      reader.close();
      String toJson = gson.toJson(info);
      FileWriter writer = new FileWriter("infoserver.json");
      writer.write(toJson);
      WordQuizzle.writeGsonLock.unlock();
      writer.close();
    } catch (FileNotFoundException e1) {
      WordQuizzle.writeGsonLock.unlock();
      e1.printStackTrace();
    } catch (IOException e) {
      WordQuizzle.writeGsonLock.unlock();
      e.printStackTrace();
    }

  }

  /**
   * Cuore della sfida, riceve la parola tradotta dall'utente e ne controlla la correttezza
   * Se le parole non sono terminate e il timer non è scaduto, allora manda la prossima 
   * parola, altrimenti se l'avversario ancora non termina restituisce un messaggio di attesa
   * altrimenti restituisce il resoconto finale
   * 
   * @param payloadString parola tradotta
   * @return un messaggo di risposta con la prossima parola se la sfida non è terminata,
   *          altrimenti un messaggio di attesa o resoconto finale
   */
  private Message sfida_during(String payloadString) {
    try {
      if (this.clientState.iterWords == 9) {
        Message[] message = new Message[1];
        for (InfoServer arg0 : info) {
          if (arg0.userName.equals(this.clientState.user.username)) {
            for (Map.Entry<ValueOne, ValueHashmap> entry : WordQuizzle.fighting.entrySet()) {
              ValueOne key = entry.getKey();
              ValueHashmap value = entry.getValue();
              if (key.equals(new ValueOne(this.clientState.user.username))) {
                value.hasFinishedFirst = true;
                WordQuizzle.fighting.replace(key, entry.getValue(), value);
                if (value.hasFinished == true) {
                  if (key.sfidaPoint > value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint + 3;
                  } else if (key.sfidaPoint < value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
                  } else {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
                  }
                } else {
                  message[0] = new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
                }
              } else if (value.equals(new ValueHashmap(this.clientState.user.username))) {
                value.hasFinished = true;
                WordQuizzle.fighting.replace(entry.getKey(), entry.getValue(), value);
                if (value.hasFinishedFirst == true) {
                  if (key.sfidaPoint > value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
                  } else if (key.sfidaPoint < value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida + 3;
                  } else {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
                  }
                } else
                    message[0] = new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
              }
            }
          }
        }
        if (message[0].getType() == Message.SFIDA_WAIT_FRIEND) return message[0];
        this.clientState.usernameSfida = null;
        updateDB(this.clientState.user.username, this.clientState.sfidaPoint);
        this.clientState.sfidaPoint = 0;
        this.clientState.correct.clear();
        this.clientState.iterWords = 0;
        this.clientState.words.clear();
        this.clientState.translatedWords.clear();
        this.clientState.user.setSfidaFalse();
        ValueOne one = new ValueOne(this.clientState.user.username);
        ValueHashmap two = new ValueHashmap(this.clientState.user.username);
        Iterator<Entry<ValueOne,ValueHashmap>> it = WordQuizzle.fighting.entrySet().iterator();
        while (it.hasNext()) {
          Entry<ValueOne,ValueHashmap> entry = it.next();
          if (entry.getKey().equals(one)) {
            if (entry.getValue().hasInterrupted == true) {
              it.remove();
            }
          }
          else if (entry.getValue().equals(two)) {
            if (entry.getValue().hasInterruptedFirst == true) {
              it.remove();
            }
          }
        }
        return message[0];
      } else if (this.clientState.iterWords == 7) {
        if (this.clientState.translatedWords.get(this.clientState.iterWords).equals(payloadString)) {
          this.clientState.correct.set(0, this.clientState.correct.get(0) + 1);
          this.clientState.sfidaPoint = this.clientState.sfidaPoint + 2;
          ValueOne one = new ValueOne(this.clientState.user.username);
          ValueHashmap two = new ValueHashmap(this.clientState.user.username);
          WordQuizzle.fighting.forEach((arg0, arg1) -> {
            if (arg0.equals(one)) {
              arg0.sfidaPoint = arg0.sfidaPoint + 2;
            } else if (arg1.equals(two)) {
              arg1.pointsSfida = arg1.pointsSfida + 2;
            }
          });
        } else {
          this.clientState.correct.set(1, this.clientState.correct.get(1) + 1);
          this.clientState.sfidaPoint = this.clientState.sfidaPoint - 1;
          ValueOne one = new ValueOne(this.clientState.user.username);
          ValueHashmap two = new ValueHashmap(this.clientState.user.username);
          WordQuizzle.fighting.forEach((arg0, arg1) -> {
            if (arg0.equals(one)) {
              arg0.sfidaPoint = arg0.sfidaPoint - 1;
            } else if (arg1.equals(two)) {
              arg1.pointsSfida = arg1.pointsSfida - 1;
            }
          });
        }
        this.clientState.iterWords = this.clientState.iterWords + 1;
        Message[] message = new Message[1];
        for (InfoServer arg0 : info) {
          if (arg0.userName.equals(this.clientState.user.username)) {
            for (Map.Entry<ValueOne, ValueHashmap> entry : WordQuizzle.fighting.entrySet()) {
              ValueOne key = entry.getKey();
              ValueHashmap value = entry.getValue();
              if (key.equals(new ValueOne(this.clientState.user.username))) {
                value.hasFinishedFirst = true;
                WordQuizzle.fighting.replace(key, entry.getValue(), value);
                if (value.hasFinished == true) {
                  if (key.sfidaPoint > value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint + 3;
                  } else if (key.sfidaPoint < value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
                  } else {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + key.sfidaPoint;
                  }
                } else {
                  message[0] = new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
                  System.out.println("ASPETTO L'ALTRO");
                }
              } else if (value.equals(new ValueHashmap(this.clientState.user.username))) {
                value.hasFinished = true;
                WordQuizzle.fighting.replace(entry.getKey(), entry.getValue(), value);
                if (value.hasFinishedFirst == true) {
                  // NON VENGONO AGGIORNATI I PUNTI DELLO SFIDATO
                  if (key.sfidaPoint > value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai perso!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
                  } else if (key.sfidaPoint < value.pointsSfida) {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, hai vinto!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida + 3;
                  } else {
                    message[0] = new Message(Message.SFIDA_FINISHING,
                        "Sfida terminata, pareggio!\nHai effettuato " + this.clientState.sfidaPoint + " punti, con "
                            + this.clientState.correct.get(0) + " risposte corrette e "
                            + this.clientState.correct.get(1) + " risposte sbagliate");
                    arg0.totalPoint = arg0.totalPoint + value.pointsSfida;
                  }
                } else {
                  message[0] = new Message(Message.SFIDA_WAIT_FRIEND,"In attesa del tuo amico");
                  System.out.println("ASPETTO L'ALTRO"); 
                }
              }
            }
          }
        }
        if (message[0].getType() == Message.SFIDA_WAIT_FRIEND) return message[0];
        this.clientState.usernameSfida = null;
        updateDB(this.clientState.user.username, this.clientState.sfidaPoint);
        this.clientState.sfidaPoint = 0;
        this.clientState.correct.clear();
        this.clientState.iterWords = 0;
        this.clientState.timer.cancel();
        this.clientState.words.clear();
        this.clientState.translatedWords.clear();
        this.clientState.user.setSfidaFalse();
        ValueOne one = new ValueOne(this.clientState.user.username);
        ValueHashmap two = new ValueHashmap(this.clientState.user.username);
        Iterator<Entry<ValueOne,ValueHashmap>> it = WordQuizzle.fighting.entrySet().iterator();
        while (it.hasNext()) {
          Entry<ValueOne,ValueHashmap> entry = it.next();
          if (entry.getKey().equals(one)) {
            if (entry.getValue().hasInterrupted == true) {
              it.remove();
            }
          }
          else if (entry.getValue().equals(two)) {
            if (entry.getValue().hasInterruptedFirst == true) {
              it.remove();
            }
          }
        }
        return message[0];
      } else {
        if (this.clientState.translatedWords.get(this.clientState.iterWords).equals(payloadString)) {
          this.clientState.iterWords = this.clientState.iterWords + 1;
          this.clientState.correct.set(0, this.clientState.correct.get(0) + 1);
          this.clientState.sfidaPoint = this.clientState.sfidaPoint + 2;
          ValueOne one = new ValueOne(this.clientState.user.username);
          ValueHashmap two = new ValueHashmap(this.clientState.user.username);
          WordQuizzle.fighting.forEach((arg0, arg1) -> {
            if (arg0.equals(one)) {
              arg0.sfidaPoint = arg0.sfidaPoint + 2;
            } else if (arg1.equals(two)) {
              arg1.pointsSfida = arg1.pointsSfida + 2;
            }
          });
          return new Message(Message.SFIDA_DURING, "right");
        } else {
          this.clientState.iterWords = this.clientState.iterWords + 1;
          this.clientState.correct.set(1, this.clientState.correct.get(1) + 1);
          this.clientState.sfidaPoint = this.clientState.sfidaPoint - 1;
          ValueOne one = new ValueOne(this.clientState.user.username);
          ValueHashmap two = new ValueHashmap(this.clientState.user.username);
          WordQuizzle.fighting.forEach((arg0, arg1) -> {
            if (arg0.equals(one)) {
              arg0.sfidaPoint = arg0.sfidaPoint - 1;
            } else if (arg1.equals(two)) {
              arg1.pointsSfida = arg1.pointsSfida - 1;
            }
          });
          return new Message(Message.SFIDA_DURING, "wrong");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new Message(Message.SFIDA_FINISHING, "aa");

  }
  /**
   * Caso in cui la richiesta di un utente non è valida
   * 
   * @return messaggio di richiesta non valida
   */
  private Message request_not_valid() {
    Message message = new Message(Message.REQUEST_NOT_VALID, "Richiesta non valida");
    return message;
  }

  /**
   * Salva il numero di porta di un utente che si connette
   * 
   * @return un messaggio di avvenuto salvataggio
   */
  private Message savePortNumber() {
    this.clientState.user.udpPort = this.clientState.requestMessage.getPayloadInt();
    Message message = new Message(Message.REQUEST_SOCKET, "saved");
    this.clientState.received.set(true);
    return message;
  }

  /**
   * Effettua il login di un utente, controlla che sia registrato, che non sia gia
   * online, e la validita della password
   * 
   * @param nickUtente nome utente 
   * @param password password utente
   * @return un messaggio di conferma oppure un messaggio di errore
   */
  @Override
  public Message login(String nickUtente, String password) {
    if (this.clientState.user.username != null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
      }
    }
    if (info == null)
      return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
    for (InfoServer s : info) {
      if (s.userName.equals(nickUtente)) {
        if (s.password.equals(password)) {
          if (this.clientState.user.isOnline() == true) {
            return new Message(Message.LOGIN_USER_ALREADY_ONLINE, "Utente gia loggato");
          } else {
            if (WordQuizzle.users.containsKey(nickUtente)) {
              return new Message(Message.LOGIN_USER_ALREADY_ONLINE, "Utente gia loggato");
            } else {
              boolean res = this.clientState.user.setOnline();
              if (res == false) {
                return new Message(Message.LOGIN_USER_ALREADY_ONLINE, "Utente gia loggato");
              } else {
                this.clientState.user.username = nickUtente;
                WordQuizzle.users.putIfAbsent(this.clientState.user.username, this.clientState.user.udpPort);
                return new Message(Message.LOGIN_OK, "Login avvenuto con successo");
              }
            }
          }
        } else {
          return new Message(Message.LOGIN_ERROR_PASSWORD, "Attenzione, password errata");
        }
      }
    }
    return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
  }

  /**
   * Consente a un utente di effettuare il logout
   * 
   * @param nickUtente nome utente
   * @return un messaggio di risposta di successo o errore
   */
  @Override
  public Message logout(String nickUtente) {
    if (this.clientState.user.username != null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
      } else {
        if (info == null)
          return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            if (this.clientState.user.isOnline() == true) {
              boolean res = this.clientState.user.setOffline();
              if (res == true) {
                WordQuizzle.users.remove(nickUtente);
                this.clientState.user.username = null;
                return new Message(Message.LOGOUT_OK, "Logout avvenuto con successo");
              } else {
                return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
              }
            }
          }
        }
        return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
      }
    } else {
      return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
    }
  }

  /**
   * Controlla se un utente è registrato
   * 
   * @param nickAmico nome amico da aggiungere
   * @return le informazioni dell'amico se è registrato altrimenti null
   */
  public InfoServer check(String nickAmico) {
    for (int i = 0; i < info.size(); i++) {
      if (info.get(i).userName.equals(nickAmico))
        return info.get(i);
    }
    return null;
  }

  /**
   * Aggiunge alla lista degli amici di nickUtente nickAmico e viceversa
   * 
   * @param nickUtente nome utente
   * @param nickAmico nome amico da aggiungere
   * @return un messaggio di successo o errore
   */
  @Override
  public Message aggiungi_amico(String nickUtente, String nickAmico) {
    if (this.clientState.user.username != null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
      } else {
        if (info == null)
          return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            if (s.friends.contains(nickAmico)) {
              return new Message(Message.ADD_FRIEND_ALREADY_FRIEND, "Amico gia presente");
            } else {
              InfoServer x;
              if ((x = check(nickAmico)) != null) {
                s.friends.add(nickAmico);
                x.friends.add(nickUtente);
                try {
                  WordQuizzle.writeGsonLock.lock();
                  Gson gson = new GsonBuilder().setPrettyPrinting().create();
                  String toJson = gson.toJson(info);
                  FileWriter writer = new FileWriter("infoserver.json");
                  writer.write(toJson);
                  WordQuizzle.writeGsonLock.unlock();
                  writer.close();
                } catch (IOException e) {
                  e.printStackTrace();
                } catch (JsonIOException e1) {
                  e1.printStackTrace();
                } catch (Exception e2) {
                  e2.printStackTrace();
                }
                return new Message(Message.ADD_FRIEND_OK, "Amico aggiunto con successo");
              } else {
                return new Message(Message.ADD_FRIEND_NOT_REGISTERED, "Amico da aggiungere non registrato");
              }
            }
          }
        }
        return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
      }
    } else {
      return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
    }
  }

  /**
   * Restituisce la lista degli amici di un utente in formato Json
   * 
   * @param nickUtente nome utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa 
   */
  @Override
  public Message lista_amici(String nickUtente) {
    if (this.clientState.user.username != null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
      } else {
        if (info == null)
          return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String payload = gson.toJson(s.friends);
            return new Message(Message.FRIENDS_LIST_OK, payload);
          }
        }
        return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
      }
    } else {
      return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
    }
  }

  /**
   * Effettua la richiesta di sfida a nickAmico e carica i dati per la sfida, 
   * altrimenti restituisce un Message di errore. Viene lanciato un Callable
   * che ritorna la risposta dell'utente sfidato
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @param nickAmico nome dell'utente che viene sfidato
   * @return un Message di risposta positiva o negativa
   */
  @Override
  public Message sfida(String nickUtente, String nickAmico) {
    // TODO Auto-generated method stub
    if (this.clientState.user.username != null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
      } else {
        if (info == null)
          return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            if (s.friends.contains(nickAmico)) {
              if (!WordQuizzle.users.containsKey(nickAmico)) {
                return new Message(Message.SFIDA_FRIEND_NOT_ONLINE, "Amico non online");
              } else {
                boolean res = this.clientState.user.setSfidaTrue();
                if (res == false) {
                  return new Message(Message.SFIDA_ALREADY, "Gia sei in sfida");
                } else {
                  this.clientState.usernameSfida = nickAmico;
                  Future<Integer> result = service
                      .submit((Callable<Integer>) new UdpRequest(WordQuizzle.users, this.clientState));
                  try {
                    if (result.get() == 0) {
                      this.clientState.user.setSfidaFalse();
                      this.clientState.usernameSfida = null;
                      return new Message(Message.SFIDA_REJECTED, "Sfida rifiutata");
                    } else if (result.get() == 2) {
                      this.clientState.user.setSfidaFalse();
                      this.clientState.usernameSfida = null;
                      return new Message(Message.SFIDA_ALREADY, "Il tuo amico è gia impegnato in una sfida");
                    } else {
                      Random rand = new Random();
                      int len = WordQuizzle.dict.size();
                      for (int i = 0; i < 8; i++) {
                        this.clientState.words.add(WordQuizzle.dict.get(rand.nextInt(len)));
                      }
                      if (WordQuizzle.fighting.containsKey(new ValueOne(this.clientState.user.username)))
                        WordQuizzle.fighting.remove(new ValueOne(this.clientState.user.username));
                      ArrayList<String> x = new ArrayList<>();
                      x.add("null");
                      ArrayList<String> tradList = translateWords(this.clientState.words);
                      if (tradList.containsAll(x)) {
                        this.clientState.user.setSfidaFalse();
                        this.clientState.words.clear();
                        WordQuizzle.fighting.put(new ValueOne(this.clientState.user.username,true),
                          new ValueHashmap(this.clientState.usernameSfida, this.clientState.words,this.clientState.translatedWords));
                        this.clientState.usernameSfida = null;
                        return new Message(Message.SFIDA_CANNOT_CONNECT,"Impossibile contattare il servizio di traduzione, sfida annullata"); 
                      }
                      this.clientState.translatedWords.addAll(tradList);
                      WordQuizzle.fighting.put(new ValueOne(this.clientState.user.username),
                          new ValueHashmap(this.clientState.usernameSfida, this.clientState.words,this.clientState.translatedWords));
                      
                      this.clientState.iterWords = 0;
                      this.clientState.correct.add(0);
                      this.clientState.correct.add(0);
                      this.clientState.timer = new Timer();
                      this.clientState.timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                          clientState.iterWords = 9;
                        }
                      }, 60000);
                      return new Message(Message.SFIDA_ACCETABLE, "Inizio sfida");
                    }
                  } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                  return new Message(Message.SFIDA_ACCETABLE, "Amico presente e online, in attesa di una risposta");
                }
              }
            } else {
              return new Message(Message.SFIDA_NOT_FRIEND, "Non siete ancora amici");
            }
          }
        }
        return new Message(Message.USER_NOT_REGISTERED, "Utente non registrato");
      }
    } else {
      return new Message(Message.USER_NOT_LOGGED, "Utente non loggato");
    }

  }

  /**
   * Traduce le 8 parole scelte casualmente per una sfida
   * 
   * @param words ArrayList di parole
   * @return le parole tradotte
   */
  private ArrayList<String> translateWords(ArrayList<String> words) {
    ArrayList<String> trad = new ArrayList<>();
    words.forEach((arg0) -> {
      URL u;
      try {
        u = new URL("https://api.mymemory.translated.net/get?q="
            + arg0 + "&langpair=it|en");
        HttpURLConnection url = (HttpURLConnection) u.openConnection();
        url.setRequestMethod("GET");
        url.setConnectTimeout(10000);
        if (url.getResponseCode() != 200) {
          trad.add("null");
        }
        else {
          BufferedReader in=new BufferedReader(new InputStreamReader(url.getInputStream()));
          String line=null;
          StringBuffer sb=new StringBuffer();
          while((line=in.readLine())!=null){
            sb.append(line);
          }
          String data = sb.toString();
          GetJson json = new Gson().fromJson(data,GetJson.class);
          trad.add(json.getResponseData().getTranslatedText());
        }
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (SocketTimeoutException e) {
        trad.add("null");
      }catch (IOException e) {
        trad.add("null");
        e.printStackTrace();
      } 
    });
    return trad;
    
  }

  /**
   * Mostra il puntegggio di nickUtente
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa
   */
  @Override
  public Message mostra_punteggio(String nickUtente) {
    if (this.clientState.user.username!=null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED,"Utente non loggato");
      } 
      else {
        if (info == null) return new Message(Message.USER_NOT_REGISTERED,"Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            //CREARE JSON E MANDARLO COME STRINGA
            return new Message(Message.SHOW_POINTS_OK,s.totalPoint);
          }
        }
        return new Message(Message.USER_NOT_REGISTERED,"Utente non registrato");
      }
    }
    else {
      return new Message(Message.USER_NOT_LOGGED,"Utente non loggato");
    }
  }

  /**
   * Mostra la classifica di nickUtente e i suoi amici in formato Json 
   * in ordine decrescente di punteggio
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa
   */
  @Override
  @SuppressWarnings("unchecked")
  public Message mostra_classifica(String nickUtente) {
    if (this.clientState.user.username!=null) {
      if (!this.clientState.user.username.equals(nickUtente)) {
        return new Message(Message.USER_NOT_LOGGED,"Utente non loggato");
      } 
      else {
        if (info == null) return new Message(Message.USER_NOT_REGISTERED,"Utente non registrato");
        for (InfoServer s : info) {
          if (s.userName.equals(nickUtente)) {
            final ArrayList<NamePoint> points = new ArrayList<>();
            ArrayList<InfoServer> c = (ArrayList<InfoServer>) info.clone();
            c.forEach((arg0) -> {
              if (s.friends.contains(arg0.userName)) {
                points.add(new NamePoint(arg0.userName,arg0.totalPoint));
              }
            });
            points.sort((NamePoint a,NamePoint b) -> {         
              return -(a.point - b.point);
            });
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String toReturn = gson.toJson(points);
            return new Message(Message.SHOW_SCORE_OK,toReturn);
          }
        }
        return new Message(Message.USER_NOT_REGISTERED,"Utente non registrato");
      }
    }
    else {
      return new Message(Message.USER_NOT_LOGGED,"Utente non loggato");
    }
  }

  
}