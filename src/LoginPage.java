import javax.swing.*;
import java.awt.*;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class LoginPage extends JFrame {
    public LoginPage() {
        setTitle("Prijava");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        JLabel userLabel = new JLabel("Uporabniško ime:");
        JTextField usernameField = new JTextField();
        JLabel passLabel = new JLabel("Geslo:");
        JPasswordField passwordField = new JPasswordField();

        JButton loginButton = new JButton("Prijava");
        JButton registerButton = new JButton("Registracija");

        add(userLabel);
        add(usernameField);
        add(passLabel);
        add(passwordField);
        add(registerButton);
        add(loginButton);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (Connection conn = DBConnection.connect()) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM uporabniki WHERE username = ?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();

                if (rs.next() && BCrypt.checkpw(password, rs.getString("pass"))) {
                    boolean isAdmin = rs.getBoolean("isadmin");
                    JOptionPane.showMessageDialog(this, "Prijava uspešna!");
                    dispose();
                    if (isAdmin) {
                        new AdminMain();
                    } else {
                        new UporabnikMain();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Napačno uporabniško ime ali geslo.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Napaka: " + ex.getMessage());
            }
        });

        registerButton.addActionListener(e -> {
            dispose(); // Zapremo trenutni login window
            new RegisterForm().setVisible(true); // Zagotovimo, da se registracijsko okno prikaže
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage());
    }
}
