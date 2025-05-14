import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DodajDomDialog extends JDialog {
    private JTextField imeField, opisField, emailField, telefonField, stSobField, naslovField;
    private JComboBox<String> krajCombo;
    private JPanel storitvePanel;
    private AdminMain parent;

    public DodajDomDialog(AdminMain parent) {
        super(parent, "Dodaj nov dom", true);
        this.parent = parent;
        setSize(400, 500);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(10, 2, 10, 10));

        // Polja
        imeField = new JTextField();
        opisField = new JTextField();
        emailField = new JTextField();
        telefonField = new JTextField();
        stSobField = new JTextField();
        naslovField = new JTextField();
        krajCombo = new JComboBox<>();
        storitvePanel = new JPanel();

        // Naloži kraje v combo box
        try (Connection conn = DBConnection.connect()) {
            PreparedStatement ps = conn.prepareStatement("SELECT ime FROM kraji ORDER BY ime");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                krajCombo.addItem(rs.getString("ime"));
            }

            // Naloži storitve v gumbne okvire (check boxe)
            PreparedStatement psStoritve = conn.prepareStatement("SELECT ime FROM storitve ORDER BY ime");
            ResultSet rsStoritve = psStoritve.executeQuery();

            storitvePanel.setLayout(new BoxLayout(storitvePanel, BoxLayout.Y_AXIS)); // Seznam v vertikalni smeri
            while (rsStoritve.next()) {
                JCheckBox storitevCheckbox = new JCheckBox(rsStoritve.getString("ime"));
                storitvePanel.add(storitevCheckbox);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Napaka pri nalaganju krajev ali storitev: " + e.getMessage());
        }

        // Dodaj komponente
        add(new JLabel("Ime doma:"));
        add(imeField);
        add(new JLabel("Opis:"));
        add(opisField);
        add(new JLabel("Email:"));
        add(emailField);
        add(new JLabel("Telefon:"));
        add(telefonField);
        add(new JLabel("Št. sob:"));
        add(stSobField);
        add(new JLabel("Naslov:"));
        add(naslovField);
        add(new JLabel("Kraj:"));
        add(krajCombo);
        add(new JLabel("Izberi storitve:"));
        add(new JScrollPane(storitvePanel));  // Dodaj JScrollPane za gumbne okvire (checkboxe)

        JButton saveBtn = new JButton("Shrani");
        JButton cancelBtn = new JButton("Prekliči");

        add(saveBtn);
        add(cancelBtn);

        saveBtn.addActionListener(e -> addHome());
        cancelBtn.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void addHome() {
        String ime = imeField.getText();
        String opis = opisField.getText();
        String email = emailField.getText();
        String telefon = telefonField.getText();
        String stSobStr = stSobField.getText();
        String naslov = naslovField.getText();
        String krajIme = (String) krajCombo.getSelectedItem();

        // Validacija
        if (ime.isEmpty() || naslov.isEmpty() || stSobStr.isEmpty() || krajIme == null) {
            JOptionPane.showMessageDialog(this, "Izpolni vsa obvezna polja.");
            return;
        }

        try {
            int stSob = Integer.parseInt(stSobStr);

            try (Connection conn = DBConnection.connect()) {
                // Poizvedba za kraj_id
                PreparedStatement psKraj = conn.prepareStatement("SELECT id FROM kraji WHERE ime = ? LIMIT 1");
                psKraj.setString(1, krajIme);
                ResultSet rs = psKraj.executeQuery();

                if (rs.next()) {
                    int krajId = rs.getInt("id");

                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO domovi (ime, opis, kraj_id, email, telefon, st_sob, naslov) VALUES (?, ?, ?, ?, ?, ?, ?)"
                    );
                    ps.setString(1, ime);
                    ps.setString(2, opis);
                    ps.setInt(3, krajId);
                    ps.setString(4, email);
                    ps.setString(5, telefon);
                    ps.setInt(6, stSob);
                    ps.setString(7, naslov);

                    ps.executeUpdate();

                    // Dobi ID doma
                    PreparedStatement psGetId = conn.prepareStatement("SELECT id FROM domovi WHERE ime = ? AND kraj_id = ? LIMIT 1");
                    psGetId.setString(1, ime);
                    psGetId.setInt(2, krajId);
                    ResultSet rsDom = psGetId.executeQuery();

                    if (rsDom.next()) {
                        int domId = rsDom.getInt("id");

                        // Dodaj izbrane storitve v povezovalno tabelo
                        java.util.List<JCheckBox> selectedServices = getSelectedServices();
                        for (JCheckBox checkbox : selectedServices) {
                            if (checkbox.isSelected()) {
                                String storitev = checkbox.getText();

                                PreparedStatement psStoritev = conn.prepareStatement("SELECT id FROM storitve WHERE ime = ?");
                                psStoritev.setString(1, storitev);
                                ResultSet rsStoritev = psStoritev.executeQuery();

                                if (rsStoritev.next()) {
                                    int storitevId = rsStoritev.getInt("id");

                                    // Vstavi v povezovalno tabelo
                                    PreparedStatement psInsertService = conn.prepareStatement(
                                            "INSERT INTO domovi_storitve (domovi_id, storitev_id) VALUES (?, ?)"
                                    );
                                    psInsertService.setInt(1, domId);
                                    psInsertService.setInt(2, storitevId);
                                    psInsertService.executeUpdate();
                                }
                            }
                        }
                    }

                    JOptionPane.showMessageDialog(this, "Dom uspešno dodan.");
                    //parent.loadHomes(); // osveži tabelo
                    dispose(); // zapri dialog
                } else {
                    JOptionPane.showMessageDialog(this, "Kraj ni najden.");
                }
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Število sob mora biti številka.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Napaka pri dodajanju doma: " + ex.getMessage());
        }
    }

    // Funkcija za pridobivanje izbranih storitev
    private List<JCheckBox> getSelectedServices() {
        List<JCheckBox> selectedServices = new ArrayList<>();
        for (Component comp : storitvePanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                selectedServices.add((JCheckBox) comp);
            }
        }
        return selectedServices;
    }
}
