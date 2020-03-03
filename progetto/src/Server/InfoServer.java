package Server;

import java.util.List;

public class InfoServer {

  /**
   * Classe InfoServer: classe utilizzata dalla libreria Gson per 
   * salvare i dati degli utenti all'interno del file infoserver.json
   * Contiene il nome, la password, la lista degli amici e il punteggio 
   * totale di un client
   */
  String userName;
  String password;
  List<String> friends;
  int totalPoint;

  public InfoServer(String userName,String password,List<String> friends,int totalPoint) {
    this.userName = userName;
    this.password = password;
    this.friends = friends;
    this.totalPoint = totalPoint;
  }
}

