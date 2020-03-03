package Shared;


public class NamePoint {

  /**
   * Classe NamePoint: classe utilizzata dal Server e dal client nel caso della classifica,
   * dove l'oggetto Json restituito è un arraylist di tipo NamePoint
   */
  public String username;
  public int point;

  public NamePoint(String user,int tot) {
    this.username = user;
    this.point = tot;
  }

  public NamePoint(String user) {
    this.username = user;
  }

  @Override
  public int hashCode() {
    return username.hashCode() + point;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NamePoint))
      return false;
    if (obj == this)
      return true;

    NamePoint rhs = (NamePoint) obj;
    return this.username.equals(rhs.username);
  }

}