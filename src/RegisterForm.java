import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterForm extends JFrame {
    public RegisterForm() {
        setTitle("Registracija");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 2));

        JLabel userLabel = new JLabel("Uporabniško ime:");
        JTextField usernameField = new JTextField();
        JLabel passLabel = new JLabel("Geslo:");
        JPasswordField passwordField = new JPasswordField();
        JLabel mailLabel = new JLabel("Email:");
        JTextField mailField = new JTextField();
        JLabel phoneLabel = new JLabel("Telefon:");
        JTextField phoneField = new JTextField();
        JLabel krajLabel = new JLabel("Kraj:");
        JComboBox<String> krajDropdown = new JComboBox<>();
        JCheckBox adminCheckBox = new JCheckBox("Admin uporabnik");
        JButton registerButton = new JButton("Registriraj se");

        // Mapa za preslikavo imena kraja v ID
        HashMap<String, Integer> krajMap = new HashMap<>();

        // Napolnimo dropdown z imeni krajev in hkrati hranimo ID-je
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, ime FROM kraji")) {
            while (rs.next()) {
                String ime = rs.getString("ime");
                int id = rs.getInt("id");
                krajDropdown.addItem(ime);
                krajMap.put(ime, id);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Napaka pri nalaganju krajev: " + e.getMessage());
        }

        add(userLabel);
        add(usernameField);
        add(passLabel);
        add(passwordField);
        add(mailLabel);
        add(mailField);
        add(phoneLabel);
        add(phoneField);
        add(krajLabel);
        add(krajDropdown);
        add(adminCheckBox);
        add(registerButton);

        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String mail = mailField.getText();
            String telefon = phoneField.getText();
            String selectedKraj = (String) krajDropdown.getSelectedItem();
            int krajId = krajMap.getOrDefault(selectedKraj, -1);
            boolean isAdmin = adminCheckBox.isSelected();

            if (krajId == -1) {
                JOptionPane.showMessageDialog(this, "Napaka: neveljaven kraj.");
                return;
            }

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            try (Connection conn = DBConnection.connect()) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO uporabniki (username, pass, mail, telefon, kraj_id, isadmin) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, mail);
                ps.setString(4, telefon);
                ps.setInt(5, krajId);
                ps.setBoolean(6, isAdmin);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Uporabnik uspešno registriran!");
                dispose();
                new LoginPage();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Napaka pri registraciji: " + ex.getMessage());
            }
        });

        setVisible(true);
    }
}