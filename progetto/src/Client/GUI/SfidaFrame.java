package Client.GUI;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import Client.ClientManager;
import Shared.Message;

public class SfidaFrame implements ActionListener {

  JFrame frame;
  JFrame homeFrame;
  JFrame startFrame;
  ClientManager client;
  JLabel l1;
  JTextField t1;
  JButton b1;
  Timer timer = new Timer();
  public int words = 0;
  JDialog dialog;

  public SfidaFrame(ClientManager client, JFrame homeFrame, JFrame startFrame) {
    this.client = client;

    this.homeFrame = homeFrame;
    this.startFrame = startFrame;

    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        b1.doClick();
      }
    }, 60000);

    frame = new JFrame();
    frame.setTitle("Sfida " + this.client.username);
    frame.setBounds(homeFrame.getX(),homeFrame.getY(), 500, 300);
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.getContentPane().setLayout(null);

    try {
      l1 = new JLabel(this.client.receiveWord().getPayloadString());
      words = words + 1;
    } catch (IOException e) {
      JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
      System.exit(1);
    }
    l1.setBounds(220, 20, 160, 30);
    frame.getContentPane().add(l1);

    t1 = new JTextField();
    t1.setBounds(200, 50, 100, 30);
    frame.getContentPane().add(t1);

    b1 = new JButton("Invia");
    b1.setBounds(390, 50, 100, 30);
    b1.addActionListener(this);
    frame.getContentPane().add(b1);

    frame.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    if (arg0.getSource() == b1) {
      String translated;
      translated = t1.getText();
      if (t1 == null) {
        translated = "";
      }
      try {
        Message message = client.sendReceive(translated);
        switch (message.getType()) {
        case Message.SFIDA_DURING:
          t1.setText("");
          l1.setText(message.getPayloadString());
          l1.repaint();
          break;
        case Message.SFIDA_FINISHING:
          client.isSfida.set(false);
          timer.cancel();
          JOptionPane.showMessageDialog(frame, message.getPayloadString(), "Sfida terminata", JOptionPane.NO_OPTION);
          frame.dispose();
          homeFrame.setVisible(true);
          break;
          /**
           * Se l'avversario non ha ancora terminato la partita attende 
           * mandando ogni secondo un messaggio per ricevere aggiornamenti sulla situazione
           */
        case Message.SFIDA_WAIT_FRIEND:
          timer.cancel();
          Message message2 = client.waitFriend();
          while (message2.getType() == Message.SFIDA_WAIT_FRIEND) {
            try {
              Thread.sleep(1000);
              message2 = client.waitFriend();
              System.out.println(message2.getType());
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
          if (message2.getType() == Message.SFIDA_FINISHING) {
            client.isSfida.set(false);
            JOptionPane.showMessageDialog(frame,message2.getPayloadString(),"Sfida terminata",JOptionPane.NO_OPTION);
            frame.dispose();
            homeFrame.setVisible(true);
          }
          break; 
        }
        
      } catch (IOException e) {
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }

  }
}