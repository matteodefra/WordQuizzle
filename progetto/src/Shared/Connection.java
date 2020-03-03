package Shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection {

  /**
   * Classe Connection: contiene la serializzazione e deserializzazione dei messaggi scambiati 
   * e i metodi di lettura e scrittura per comunicare sul SocketChannel
   */


  /**
   * Legge dal SocketChannel la lunghezza del messaggio e il messaggio, ritorna 
   * un oggetto di tipo Message tramite la deserializzazione
   * 
   * @param channel socket da dove leggere
   * @return messaggio letto deserializzato
   * @throws IOException se il canale è stato chiuso
   */
  public static Message read(SocketChannel channel) throws IOException {
    
    ByteBuffer dimensions = ByteBuffer.allocate(4);
    
    while (dimensions.hasRemaining()) {
      int numByte = channel.read(dimensions);
      if (numByte == -1) {
        channel.close();
      }
    }

    dimensions.flip();
    ByteBuffer message = ByteBuffer.allocate(dimensions.getInt(0));
    while (message.hasRemaining()) {
      int numByte = channel.read(message);
      if (numByte == -1) {
        channel.close();
      }
    }

    return (Message) getObjectFromByte(message.array());
  }


  /**
   * Scrive sul SocketChannel la lunghezza del messaggio e il messaggio serializzato
   * 
   * @param channel socket dove scrivere
   * @param message messaggio da scrivere 
   * @return messaggio da scrivere
   * @throws IOException se il canale è stato chiuso 
   */
  public static Message write(SocketChannel channel,Message message) throws IOException {
    if (channel == null || message == null) throw new IOException();

    byte[] b = getByteFromObject(message);

    ByteBuffer size = ByteBuffer.allocate(4);
    size.putInt(b.length);

    size.flip();

    while (size.hasRemaining()) {
      int numByte = channel.write(size);
      if (numByte == -1) {
        channel.close();
      }
    }
    
    ByteBuffer messageBuffer = ByteBuffer.wrap(b);
    while (messageBuffer.hasRemaining()) {
      int numByte = channel.write(messageBuffer);
      if (numByte == -1) {
        channel.close();
      }
    }

    return message;

  }

  /**
    * Serializzazione oggetto
    * 
    * @param o oggetto da serializzare
    * @return byte[] serializzato
    */	
  private static byte[] getByteFromObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		try(ObjectOutputStream out = new ObjectOutputStream(bos);){
			out.writeObject(o);
			out.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
       }
		return bos.toByteArray();
		
	}
	
  /**
  * Deserializzazione oggetto
  * 
  * @param b byte da deserializzare
  * @return oggetto de-serializzato
  */	
	private static Object getObjectFromByte(byte[] b) {
		ByteArrayInputStream bis = new ByteArrayInputStream(b);
		
		try(ObjectInputStream in = new ObjectInputStream(bis);){
			return in.readObject();
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
       }
		catch(ClassNotFoundException ex){
			ex.printStackTrace();
			return null;
		}
  }
  
}