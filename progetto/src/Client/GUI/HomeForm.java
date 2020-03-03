package Client.GUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Client.ClientManager;
import Shared.Message;
import Shared.NamePoint;

public class HomeForm implements ActionListener {

  /**
   * Home Form, consente a un utente di effettuare le varie operazioni disponibili
   */
  public static JFrame frame;
  JFrame startFrame;
  public JButton sfida;
  public JButton addFriend;
  public JButton listFriends;
  public static JButton showScore;
  public JButton showScoreBoard;
  public JButton logoutButton;
  public JButton sfidaStart;
  public ClientManager client;
  public Thread thread;
  public Thread waitThread;

  public HomeForm(ClientManager client,JFrame startFrame,Thread thread) {
    this.client = client;
    this.startFrame = startFrame;
    this.thread = thread;

    frame = new JFrame();
    frame.setTitle("WORDQUIZZLE");
    frame.setBounds(startFrame.getX(),startFrame.getY(), 400, 400);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          client.logout();
          System.exit(1);
        } catch (IOException e1) {
          JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
          System.exit(1);
        }
      }
    });
    frame.getContentPane().setLayout(null);

    JLabel option = new JLabel("Ciao " + client.username + "! Cosa vuoi fare?",SwingConstants.CENTER);
    option.setBounds(100, 20, 200, 21);
    frame.getContentPane().add(option);
    frame.setVisible(true);

    sfida = new JButton("Sfida amico");
    sfida.setBounds(25,100,150,40);
    sfida.addActionListener(this);
    frame.getContentPane().add(sfida);

    sfidaStart = new JButton("Inizia sfida");
    sfidaStart.setBounds(225,100,150,40);
    sfidaStart.addActionListener(this);
    frame.getContentPane().add(sfidaStart);
    
    addFriend = new JButton("Aggiungi amico");
    addFriend.setBounds(25, 150, 150, 40);
    addFriend.addActionListener(this);
    frame.getContentPane().add(addFriend);

    listFriends = new JButton("Lista Amici");
    listFriends.setBounds(225, 150, 150, 40);
    listFriends.addActionListener(this);
    frame.getContentPane().add(listFriends);

    showScore = new JButton("Punteggio");
    showScore.setBounds(25, 250, 150, 40);
    showScore.addActionListener(this);
    frame.getContentPane().add(showScore);

    showScoreBoard = new JButton("Classifica");
    showScoreBoard.setBounds(225, 250, 150, 40);
    showScoreBoard.addActionListener(this);
    frame.getContentPane().add(showScoreBoard);

    logoutButton = new JButton("Logout");
    logoutButton.setBounds(125, 300, 150, 40);
    logoutButton.addActionListener(this);
    frame.getContentPane().add(logoutButton);

    frame.setVisible(true);

  }

  @Override
  @SuppressWarnings("unused")
  public void actionPerformed(ActionEvent arg0) {
    if (arg0.getSource() == addFriend) {
      String friend;
      friend = JOptionPane.showInputDialog(frame, "Inserisci il nome dell'amico", "Aggiungi amico",
                JOptionPane.QUESTION_MESSAGE);
      if (friend == null || friend.isEmpty()){
        JOptionPane.showMessageDialog(frame,"Inserire nome","Attenzione",JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      try {
        int code = client.addFriend(friend);
        switch (code) {
          case Message.ADD_FRIEND_ALREADY_FRIEND: 
            JOptionPane.showMessageDialog(frame, "Amico gia presente", "Attenzione", JOptionPane.INFORMATION_MESSAGE);
            break;
          case Message.ADD_FRIEND_NOT_REGISTERED:
            JOptionPane.showMessageDialog(frame,"Amico non ancora registrato", "Attenzione", JOptionPane.ERROR_MESSAGE);
            break;
          case Message.ADD_FRIEND_OK:
            JOptionPane.showMessageDialog(frame,"Amico aggiunto con successo", "Successo",JOptionPane.OK_OPTION);
            break;
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Sfida annullata",JOptionPane.ERROR_MESSAGE);
            break;
          default:
            JOptionPane.showMessageDialog(frame,"Errore nella richiesta","Errore",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == listFriends) {
      try {
        Message code = client.showFriends();
        switch (code.getType()) {
          case Message.FRIENDS_LIST_OK:
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type list = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> friends = gson.fromJson(code.getPayloadString(),list);
            StringBuilder builder = new StringBuilder();
            for (String s : friends) {
              builder.append(s + "\n");
            }
            JOptionPane.showMessageDialog(frame,builder.toString(),"Successo", JOptionPane.NO_OPTION);
            break;
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Sfida annullata",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == showScore) {
      try {
        Message code = client.showScore();
        switch (code.getType()) {
          case Message.SHOW_POINTS_OK:
            JOptionPane.showMessageDialog(frame,code.getPayloadInt(),"Successo",JOptionPane.NO_OPTION);
            break;
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Sfida annullata",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == sfidaStart) {
      try {
        if (client.queue.isEmpty()) {
          JOptionPane.showMessageDialog(frame,"Nessuna richiesta di sfida","Info",JOptionPane.INFORMATION_MESSAGE);
          return;
        }
        else {
          Message code = client.reqNotValid();
          switch (code.getType()) {
            case Message.SFIDA_ACCETABLE:
              client.queue.remove(); 
              client.isSfida.set(true);
              JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
              frame.setVisible(false);
              SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
              break;
            case Message.REQUEST_NOT_VALID:
              JOptionPane.showMessageDialog(frame,"Preparazione della sfida, riprova tra poco","Info",JOptionPane.INFORMATION_MESSAGE);
              break;
          }
        }
        
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == showScoreBoard) {
      try {
        Message code = client.showScoreBoard();
        switch (code.getType()) {
          case Message.SHOW_SCORE_OK:
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type list = new TypeToken<ArrayList<NamePoint>>(){}.getType();
            ArrayList<NamePoint> points = gson.fromJson(code.getPayloadString(),list);
            StringBuilder builder = new StringBuilder();
            for (NamePoint p : points) {
              builder.append(p.username + " " + p.point + "\n");
            }
            JOptionPane.showMessageDialog(frame,builder.toString(),"Successo",JOptionPane.NO_OPTION);
            break;
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Sfida annullata",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == logoutButton) {
      try {
        int code = client.logout();
        switch (code) {
          case Message.LOGOUT_OK:
            JOptionPane.showMessageDialog(frame,"Logout avvenuto con successo","Successo",JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            this.client.username = null;
            startFrame.setVisible(true);
            break;
          case Message.LOGOUT_ERR:
            JOptionPane.showMessageDialog(frame,"Errore in logout","Errore",JOptionPane.ERROR_MESSAGE);
            break;
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Sfida annullata",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

    else if (arg0.getSource() == sfida) {
      try {
        String friend;
        friend = JOptionPane.showInputDialog(frame, "Inserisci il nome dell'amico", "Sfida amico",
                JOptionPane.QUESTION_MESSAGE);
        if (friend == null) {
          JOptionPane.showMessageDialog(frame,"Inserire nome","Attenzione",JOptionPane.INFORMATION_MESSAGE);
          return;
        }
        int code = client.sfida(friend);
        switch (code) {
          case Message.SFIDA_ACCETABLE:
            client.isSfida.set(true);
            JOptionPane.showMessageDialog(frame,"Sfida accettata! Tieniti pronto..","Prepararsi",JOptionPane.NO_OPTION);
            frame.setVisible(false);
            SfidaFrame sfidaFrame = new SfidaFrame(client,frame,startFrame);
            break;
          case Message.SFIDA_ALREADY: 
            JOptionPane.showMessageDialog(frame,"Il tuo amico è gia impegnato in una sfida","Attenzione",JOptionPane.INFORMATION_MESSAGE);
            break;
          case Message.SFIDA_FRIEND_NOT_ONLINE:
            JOptionPane.showMessageDialog(frame,"Il tuo amico non è online","Attenzione",JOptionPane.INFORMATION_MESSAGE);
            break;
          case Message.SFIDA_NOT_FRIEND:
            JOptionPane.showMessageDialog(frame,"Non avete ancora stretto amicizia","Attenzione",JOptionPane.INFORMATION_MESSAGE);
            break;
          case Message.SFIDA_REJECTED: 
            JOptionPane.showMessageDialog(frame,"Il tuo amico ha rifiutato la sfida","Attenzione",JOptionPane.ERROR_MESSAGE);
            break;
          case Message.SFIDA_CANNOT_CONNECT:
            JOptionPane.showMessageDialog(frame,"Impossibile contattare il servizio di traduzione","Errore",JOptionPane.ERROR_MESSAGE);
            break;
        }

      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

  }

}
