package Shared;

public class ValueOne {

  /**
   * Classe ValueOne: oggetto usato come chiave della ConcurrentHashmap per la gestione
   * delle sfide lato server. Contiene il nome del client che sfida, i punti effettuati,
   * una variabile booleana isInterrupted per controllare se si sono verificati degli errori
   * nella traduzione delle parole
   */
  public String user;
  public int sfidaPoint;
  public boolean isInterrupted;

  public ValueOne(String user) {
    this.user = user;
    this.isInterrupted = false;
  }

  public ValueOne(String user,boolean isInterrupted) {
    this.user = user;
    this.isInterrupted = isInterrupted;
  }

  /**
   * Override dei metodi equals e hashcode per verificare che un utente sia impegnato in una sfida
   * controlla solamente che il suo nome compaia tra le chiavi dell'Hashmap
   */
  @Override
  public int hashCode() {
    if (this.user == null) return 0;  
    else return this.user.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ValueOne))
      return false;
    if (obj == this)
      return true;

    ValueOne rhs = (ValueOne) obj;
    return this.user.equals(rhs.user);
  }
}