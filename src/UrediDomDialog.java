import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class UrediDomDialog extends JDialog {
    private JTextField imeField, opisField, emailField, telefonField, stSobField, naslovField;
    private JComboBox<String> krajCombo;
    private JPanel storitvePanel;
    private HashMap<String, Integer> storitevMap = new HashMap<>();
    private ArrayList<JCheckBox> storitevCheckboxes = new ArrayList<>();
    private AdminMain parent;
    private int domId;

    public UrediDomDialog(AdminMain parent, int domId) {
        super(parent, "Uredi dom", true);
        this.parent = parent;
        this.domId = domId;
        setSize(500, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        imeField = new JTextField();
        opisField = new JTextField();
        emailField = new JTextField();
        telefonField = new JTextField();
        stSobField = new JTextField();
        naslovField = new JTextField();
        krajCombo = new JComboBox<>();

        formPanel.add(new JLabel("Ime doma:"));
        formPanel.add(imeField);
        formPanel.add(new JLabel("Opis:"));
        formPanel.add(opisField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Telefon:"));
        formPanel.add(telefonField);
        formPanel.add(new JLabel("Št. sob:"));
        formPanel.add(stSobField);
        formPanel.add(new JLabel("Naslov:"));
        formPanel.add(naslovField);
        formPanel.add(new JLabel("Kraj:"));
        formPanel.add(krajCombo);

        add(formPanel, BorderLayout.NORTH);

        // Panel za storitve
        storitvePanel = new JPanel();
        storitvePanel.setLayout(new BoxLayout(storitvePanel, BoxLayout.Y_AXIS));
        storitvePanel.setBorder(BorderFactory.createTitledBorder("Storitve"));

        JScrollPane storitveScroll = new JScrollPane(storitvePanel);
        storitveScroll.setPreferredSize(new Dimension(400, 150));
        add(storitveScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        JButton saveBtn = new JButton("Shrani");
        JButton cancelBtn = new JButton("Prekliči");
        bottomPanel.add(saveBtn);
        bottomPanel.add(cancelBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        saveBtn.addActionListener(e -> updateDom());
        cancelBtn.addActionListener(e -> dispose());

        loadData();

        setVisible(true);
    }

    private void loadData() {
        try (Connection conn = DBConnection.connect()) {
            // Naloži kraje
            PreparedStatement psKraji = conn.prepareStatement("SELECT ime FROM kraji ORDER BY ime");
            ResultSet rsKraji = psKraji.executeQuery();
            while (rsKraji.next()) {
                krajCombo.addItem(rsKraji.getString("ime"));
            }

            // Naloži obstoječe podatke doma
            PreparedStatement ps = conn.prepareStatement("""
                SELECT d.ime, d.opis, d.email, d.telefon, d.st_sob, d.naslov, k.ime AS kraj
                FROM domovi d JOIN kraji k ON d.kraj_id = k.id WHERE d.id = ?
            """);
            ps.setInt(1, domId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                imeField.setText(rs.getString("ime"));
                opisField.setText(rs.getString("opis"));
                emailField.setText(rs.getString("email"));
                telefonField.setText(rs.getString("telefon"));
                stSobField.setText(String.valueOf(rs.getInt("st_sob")));
                naslovField.setText(rs.getString("naslov"));
                krajCombo.setSelectedItem(rs.getString("kraj"));
            }

            // Naloži vse storitve
            PreparedStatement psStoritve = conn.prepareStatement("SELECT id,ime FROM storitve ORDER BY ime");
            ResultSet rsStoritve = psStoritve.executeQuery();
            while (rsStoritve.next()) {
                int id = rsStoritve.getInt("id");
                String ime = rsStoritve.getString("ime");
                storitevMap.put(ime, id);

                JCheckBox cb = new JCheckBox(ime);
                storitevCheckboxes.add(cb);
                storitvePanel.add(cb);
            }

            // Označi obstoječe storitve tega doma
            PreparedStatement psObstoj = conn.prepareStatement("""
                SELECT s.ime FROM storitve s
                JOIN domovi_storitve ds ON s.id = ds.storitev_id
                WHERE ds.domovi_id = ?
            """);
            psObstoj.setInt(1, domId);
            ResultSet rsObstoj = psObstoj.executeQuery();
            while (rsObstoj.next()) {
                String storitev = rsObstoj.getString("ime");
                for (JCheckBox cb : storitevCheckboxes) {
                    if (cb.getText().equals(storitev)) {
                        cb.setSelected(true);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Napaka pri nalaganju podatkov: " + e.getMessage());
            dispose();
        }
    }

    private void updateDom() {
        String ime = imeField.getText();
        String opis = opisField.getText();
        String email = emailField.getText();
        String telefon = telefonField.getText();
        String stSobStr = stSobField.getText();
        String naslov = naslovField.getText();
        String krajIme = (String) krajCombo.getSelectedItem();

        if (ime.isEmpty() || naslov.isEmpty() || stSobStr.isEmpty() || krajIme == null) {
            JOptionPane.showMessageDialog(this, "Izpolni vsa obvezna polja.");
            return;
        }

        try {
            int stSob = Integer.parseInt(stSobStr);

            try (Connection conn = DBConnection.connect()) {
                conn.setAutoCommit(false);

                // Poišči kraj_id
                PreparedStatement psKraj = conn.prepareStatement("SELECT id FROM kraji WHERE ime = ? LIMIT 1");
                psKraj.setString(1, krajIme);
                ResultSet rs = psKraj.executeQuery();

                if (rs.next()) {
                    int krajId = rs.getInt("id");

                    // Posodobi dom
                    PreparedStatement ps = conn.prepareStatement("""
                        UPDATE domovi SET ime = ?, opis = ?, email = ?, telefon = ?, st_sob = ?, naslov = ?, kraj_id = ? WHERE id = ?
                    """);
                    ps.setString(1, ime);
                    ps.setString(2, opis);
                    ps.setString(3, email);
                    ps.setString(4, telefon);
                    ps.setInt(5, stSob);
                    ps.setString(6, naslov);
                    ps.setInt(7, krajId);
                    ps.setInt(8, domId);
                    ps.executeUpdate();

                    // Najprej izbriši obstoječe storitve
                    PreparedStatement deleteStoritve = conn.prepareStatement("DELETE FROM domovi_storitve WHERE domovi_id = ?");
                    deleteStoritve.setInt(1, domId);
                    deleteStoritve.executeUpdate();

                    // Dodaj nove izbrane storitve
                    PreparedStatement insertStoritve = conn.prepareStatement("INSERT INTO domovi_storitve (domovi_id, storitev_id) VALUES (?, ?)");
                    for (JCheckBox cb : storitevCheckboxes) {
                        if (cb.isSelected()) {
                            int storitevId = storitevMap.get(cb.getText());
                            insertStoritve.setInt(1, domId);
                            insertStoritve.setInt(2, storitevId);
                            insertStoritve.addBatch();
                        }
                    }
                    insertStoritve.executeBatch();

                    conn.commit();

                    JOptionPane.showMessageDialog(this, "Dom uspešno posodobljen.");
                    //parent.loadHomes();
                    dispose();

                } else {
                    JOptionPane.showMessageDialog(this, "Kraj ni najden.");
                    conn.rollback();
                }

            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Število sob mora biti številka.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Napaka pri posodabljanju doma: " + ex.getMessage());
        }
    }
}
