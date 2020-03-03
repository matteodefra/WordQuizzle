package Server;

import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import Shared.Message;

public class ClientState {

  /**
   * Classe ClientState: è l'attachment di ogni client, viene creato 
   * al momento della connessione. Contiene:
   * @param requestMessage messaggio di richiesta del client
   * @param responseMessage messaggio di risposta da mandare al client
   * @param user informazioni dell'utente
   * @param usernameSfida settato quando il client manda una richiesta di sfida
   * @param words contiene le parole da tradurre relative alla sfida in corso se c'è
   * @param translatedWords contiene le parole tradotte relative alla sfida in corso se c'è
   * @param iterWords per scorrere l'array delle parole e di quelle tradotte
   * @param sfidaPoint per tenere conto dei punti effettuati da un utente durante una sfida
   * @param correct array di due posizioni per salvare il numero delle risposte corrette e non
   * @param timer per terminare la sfida se il timer è scaduto
   */
  Message requestMessage;
  Message responseMessage;
  User user;
  String usernameSfida;
  AtomicBoolean received = new AtomicBoolean(false);
  ArrayList<String> words;
  ArrayList<String> translatedWords;
  int iterWords;
  int sfidaPoint;
  ArrayList<Integer> correct;
  Timer timer;

  public ClientState(User user) {
    this.user = user;
    this.words = new ArrayList<>();
    this.translatedWords = new ArrayList<>();
    this.correct = new ArrayList<>();
  }

}