package Server;


import Shared.Message;

/**
 * Interfaccia WordQuizzleInterface: contiene tutte le dichiarazioni di funzioni implementati dal server
 */
public interface WordQuizzleInterface {
  /**
   * Funzione per effettuare il login di un utente
   * 
   * @param nickUtente nome utente
   * @param password password utente
   * @return un Message di risposta positiva o negativa
   */
  public Message login(String nickUtente, String password);

  /**
   * Funzione per effettuare il logout di un utente
   * 
   * @param nickUtente nome utente
   * @return un Message di risposta positiva o negativa
   */
  public Message logout(String nickUtente);

  /**
   * Aggiunge un amico alla lista degli amici di un utente. 
   * L'aggiunta è fatta per entrambi gli utenti
   * 
   * @param nickUtente nome utente che effettua la richiesta
   * @param nickAmico nome utente che si vuole aggiungere
   * @return un Message di risposta positiva o negativa
   */
  public Message aggiungi_amico(String nickUtente, String nickAmico);

  /**
   * Restituisce la lista degli amici di un utente in formato Json
   * 
   * @param nickUtente nome utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa 
   */
  public Message lista_amici(String nickUtente);

  /**
   * Effettua la richiesta di sfida a nickAmico e carica i dati per la sfida, 
   * altrimenti restituisce un Message di errore
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @param nickAmico nome dell'utente che viene sfidato
   * @return un Message di risposta positiva o negativa
   */
  public Message sfida(String nickUtente,String nickAmico);

  /**
   * Mostra il puntegggio di nickUtente
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa
   */
  public Message mostra_punteggio(String nickUtente);

  /**
   * Mostra la classifica di nickUtente e i suoi amici in formato Json 
   * in ordine decrescente di punteggio
   * 
   * @param nickUtente nome dell'utente che effettua la richiesta
   * @return un Message di risposta positiva o negativa
   */
  public Message mostra_classifica(String nickUtente);

}
