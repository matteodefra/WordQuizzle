package Shared;

import java.util.ArrayList;

public class ValueHashmap {

  /**
   * Class ValueHashmap: oggetto che viene usato nelle sfide, all'inizio di una sfida viene 
   * aggiunto come valore di una ConcurrentHashMap con il nome dello sfidato, le parole 
   * da tradurre, le parole tradotte dal server, i punti effettuati dallo sfidato, due 
   * variabili booleane per sapere se ha finito lo sfidante e lo sfidato per la sincronizzazione 
   * della sfida e un booleano per sapere se la sfida deve essere interrotta, e come valore 
   * il nome dello sfidato, le parole da tradurre e le parole tradotte, i punti effettuati dallo 
   * sfidato e due booleani per sincronizzare la fine della sfida tra i due utenti
   */
  public String user;
  public ArrayList<String> sfidaWords;
  public ArrayList<String> translated;
  public int pointsSfida;
  public boolean hasFinished;
  public boolean hasFinishedFirst;
  public boolean hasInterrupted;
  public boolean hasInterruptedFirst;

  public ValueHashmap(String user) {
    this.user = user;
    this.hasFinished = false;
    this.hasFinishedFirst = false;
    this.hasInterrupted = false;
    this.hasInterruptedFirst = false;
  }

  public ValueHashmap(String user,ArrayList<String> words) {
    this.user = user;
    sfidaWords = new ArrayList<>();
    sfidaWords.addAll(words);
    this.hasFinished = false;
    this.hasFinishedFirst = false;
    this.hasInterrupted = false;
    this.hasInterruptedFirst = false;
  }

  public ValueHashmap(String user,ArrayList<String> words,ArrayList<String> translated) {
    this.user = user;
    this.hasFinished = false;
    this.hasFinishedFirst = false;
    this.hasInterrupted = false;
    this.hasInterruptedFirst = false;
    sfidaWords = new ArrayList<>();
    sfidaWords.addAll(words);
    this.translated = new ArrayList<>();
    this.translated.addAll(translated);
  }

  /**
   * Override dei metodi equals e hashcode per verificare se un utente è impegnato in una sfida
   * controlla solamente il valore del nome che corrisponda
   */
  @Override
  public int hashCode() {
    return user != null ? user.hashCode() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ValueHashmap))
      return false;
    if (obj == this)
      return true;

    ValueHashmap rhs = (ValueHashmap) obj;
    return this.user.equals(rhs.user);
  }
}