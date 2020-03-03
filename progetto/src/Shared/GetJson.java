package Shared;

public class GetJson {

  /**
   * Classe GetJson: Classe per leggere la risposta del sito api.mymemory.translated
   * per il recupero della parola tradotta
   */
  Info responseData;

  public class Info {

    String translatedText;

    /**
     * 
     * @return stringa tradota
     */
    public String getTranslatedText() {
      return translatedText;
    }
  }

  public Info getResponseData() {
    return responseData;
  }
}