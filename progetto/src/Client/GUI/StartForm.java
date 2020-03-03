package Client.GUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import Client.ClientManager;
import Client.UdpClient;
import Shared.Message;

public class StartForm implements ActionListener {

  /**
   * Form iniziale, consente a un client di effettuare il login o la registrazione
   */
  private JFrame frame;
  private JTextField usernameField;
  private JPasswordField passwordField;
  private JButton btnLogin;
  private JButton registerButton;
  private ClientManager client;

  public StartForm(ClientManager client) {
    initialize();
    frame.setVisible(true);
    this.client = client;
  }

  private void initialize() {
    frame = new JFrame();
    frame.setTitle("WORDQUIZZLE");
    frame.setBounds(100, 100, 415, 271);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(null);

    JLabel lblUsername = new JLabel("Username");
    lblUsername.setBounds(73, 73, 105, 21);
    frame.getContentPane().add(lblUsername);

    JLabel lblPassword = new JLabel("Password");
    lblPassword.setBounds(73, 118, 86, 14);
    frame.getContentPane().add(lblPassword);

    usernameField = new JTextField();
    usernameField.setBounds(188, 73, 115, 21);
    frame.getContentPane().add(usernameField);
    usernameField.setColumns(10);

    passwordField = new JPasswordField();
    passwordField.setBounds(188, 118, 115, 21);
    frame.getContentPane().add(passwordField);

    btnLogin = new JButton("Login");
    btnLogin.addActionListener(this);
    btnLogin.setBounds(73, 175, 89, 23);
    frame.getContentPane().add(btnLogin);

    registerButton = new JButton("Registrati");
    registerButton.addActionListener(this);
    registerButton.setBounds(188, 175, 115, 23);
    frame.getContentPane().add(registerButton);

  }

  @Override
  @SuppressWarnings("unused")
  @Deprecated
  public void actionPerformed(ActionEvent arg0) {
    if (arg0.getSource() == btnLogin) {
      if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
        JOptionPane.showMessageDialog(frame, "Tutti i campi devono essere riempiti", "Errore",
            JOptionPane.ERROR_MESSAGE);
        return;
      }
      try {
        int res = client.login(usernameField.getText().trim(), passwordField.getText());
        switch (res) {
          /**
           * Se il login ha avuto successo viene lanciata la HomeForm e viene avviato il Thread UDP
           * per ricevere richieste di sfida
           */
          case Message.LOGIN_OK:
            Thread x = new Thread(new UdpClient(this.client,this.client.datagramSocket,this.client.registration,this.client.username,this.frame));
            x.start();
            JOptionPane.showMessageDialog(frame, "Login avvenuto con successo", "Successo", JOptionPane.INFORMATION_MESSAGE);
            usernameField.setText(null);
            passwordField.setText(null);
            frame.setVisible(false);
            HomeForm homeForm = new HomeForm(client,frame,x);
            break;
          case Message.LOGIN_ERROR_PASSWORD:
            JOptionPane.showMessageDialog(frame,"Password errata","Attenzione",JOptionPane.ERROR_MESSAGE);
            break;
          case Message.LOGIN_USER_ALREADY_ONLINE:
            JOptionPane.showMessageDialog(frame,"Utente gia loggato","Attenzione",JOptionPane.ERROR_MESSAGE);
            break;
          case Message.USER_NOT_REGISTERED:
            JOptionPane.showMessageDialog(frame,"Utente non ancora registrato","Attenzione",JOptionPane.ERROR_MESSAGE);
            break;
        }
      } catch (IOException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(frame,"Server disconnesso","Errore",JOptionPane.INFORMATION_MESSAGE);
        System.exit(1);
      }
    }
    else if (arg0.getSource() == registerButton) {
      if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
        JOptionPane.showMessageDialog(frame, "Tutti i campi devono essere riempiti", "Errore", JOptionPane.ERROR_MESSAGE);
        return;
      }
      try {
        int res = client.register(usernameField.getText().trim(), passwordField.getText());
        if (res == 1) {
          JOptionPane.showMessageDialog(frame,"Registrazione avvenuta con successo, fai subito il primo login","Sucesso",JOptionPane.OK_OPTION);
        }
        else {
          JOptionPane.showMessageDialog(frame,"Utente gia registrato","Errore",JOptionPane.ERROR_MESSAGE);
        } 
      } catch (RemoteException e) {
        e.printStackTrace();
      }

    }

  }
}