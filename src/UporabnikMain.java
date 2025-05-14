import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class UporabnikMain extends JFrame {
    private JTable domoviTable;

    public UporabnikMain() {
        setTitle("Domovi za ostarele");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        domoviTable = new JTable();
        JScrollPane tableScroll = new JScrollPane(domoviTable);
        add(tableScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton oceniBtn = new JButton("Oceni dom");
        JButton komentarjiBtn = new JButton("Poglej komentarje");
        bottomPanel.add(oceniBtn);
        bottomPanel.add(komentarjiBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        oceniBtn.addActionListener(e -> {
            int row = domoviTable.getSelectedRow();
            if (row >= 0) {
                String imeDoma = domoviTable.getValueAt(row, 0).toString();
                new OcenaDialog(this, imeDoma);
            } else {
                JOptionPane.showMessageDialog(this, "Najprej izberite dom.");
            }
        });

        komentarjiBtn.addActionListener(e -> {
            int row = domoviTable.getSelectedRow();
            if (row >= 0) {
                String imeDoma = domoviTable.getValueAt(row, 0).toString();
                new KomentarDialog(this, imeDoma);
            } else {
                JOptionPane.showMessageDialog(this, "Najprej izberite dom.");
            }
        });

        loadHomes();
        setVisible(true);
    }

    private void loadHomes() {
        try (Connection conn = DBConnection.connect()) {
            String sql = """
                SELECT d.ime, d.naslov, k.ime AS kraj, d.telefon, d.email, d.st_sob,
                       ROUND(COALESCE(AVG(o.ocena), 0), 2) AS povprecna_ocena
                FROM domovi d
                JOIN kraji k ON d.kraj_id = k.id
                LEFT JOIN ocene o ON d.id = o.dom_id
                GROUP BY d.id, d.ime, d.naslov, k.ime, d.telefon, d.email, d.st_sob
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            Vector<String> columnNames = new Vector<>();
            for (int column = 1; column <= columnCount; column++) {
                columnNames.add(metaData.getColumnLabel(column));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    row.add(rs.getObject(columnIndex));
                }
                data.add(row);
            }

            domoviTable.setModel(new DefaultTableModel(data, columnNames));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Napaka pri nalaganju domov: " + e.getMessage());
        }
    }

    private static class OcenaDialog extends JDialog {
        public OcenaDialog(JFrame parent, String imeDoma) {
            super(parent, "Oceni dom: " + imeDoma, true);
            setSize(400, 200);
            setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
            form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            form.add(new JLabel("Ocena (1–5):"));
            JComboBox<Integer> ocenaBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
            form.add(ocenaBox);
            form.add(new JLabel("Komentar:"));
            JTextField komentarField = new JTextField();
            form.add(komentarField);

            JButton submitBtn = new JButton("Pošlji");
            add(form, BorderLayout.CENTER);
            add(submitBtn, BorderLayout.SOUTH);

            submitBtn.addActionListener(e -> {
                int ocena = (int) ocenaBox.getSelectedItem();
                String komentar = komentarField.getText();

                try (Connection conn = DBConnection.connect()) {
                    String getDomIdSql = "SELECT id FROM domovi WHERE ime = ?";
                    PreparedStatement getId = conn.prepareStatement(getDomIdSql);
                    getId.setString(1, imeDoma);
                    ResultSet rs = getId.executeQuery();
                    if (rs.next()) {
                        int domId = rs.getInt("id");
                        String insert = "INSERT INTO ocene (dom_id, uporabnik_id, ocena, komentar) VALUES (?, ?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(insert);
                        ps.setInt(1, domId);
                        ps.setInt(2, 1);
                        ps.setInt(3, ocena);
                        ps.setString(4, komentar);
                        ps.executeUpdate();

                        JOptionPane.showMessageDialog(this, "Ocena shranjena.");
                        dispose();
                        ((UporabnikMain) parent).loadHomes();
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Napaka pri shranjevanju ocene: " + ex.getMessage());
                }
            });

            setLocationRelativeTo(parent);
            setVisible(true);
        }
    }

    private static class KomentarDialog extends JDialog {
        public KomentarDialog(JFrame parent, String imeDoma) {
            super(parent, "Komentarji za dom: " + imeDoma, true);
            setSize(500, 500);
            setLayout(new BorderLayout());

            JTextArea komentarArea = new JTextArea();
            komentarArea.setEditable(false);
            JScrollPane scroll = new JScrollPane(komentarArea);
            add(scroll, BorderLayout.CENTER);

            try (Connection conn = DBConnection.connect()) {
                // Najprej pridobimo storitve
                String storitveSql = """
                SELECT s.ime
                FROM storitve s
                JOIN domovi_storitve ds ON s.id = ds.storitev_id
                JOIN domovi d ON ds.domovi_id = d.id
                WHERE d.ime = ?
            """;
                PreparedStatement storPs = conn.prepareStatement(storitveSql);
                storPs.setString(1, imeDoma);
                ResultSet storRs = storPs.executeQuery();

                StringBuilder sb = new StringBuilder();
                sb.append("Storitve doma:\n");
                boolean imaStoritve = false;
                while (storRs.next()) {
                    imaStoritve = true;
                    sb.append("- ").append(storRs.getString("ime")).append("\n");
                }
                if (!imaStoritve) {
                    sb.append("Ni navedenih storitev.\n");
                }

                sb.append("\nKomentarji:\n");

                // Nato pridobimo komentarje
                String sql = """
                SELECT o.ocena, o.komentar, u.username
                FROM ocene o
                JOIN domovi d ON o.dom_id = d.id
                JOIN uporabniki u ON o.uporabnik_id = u.id
                WHERE d.ime = ?
            """;
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, imeDoma);
                ResultSet rs = ps.executeQuery();

                boolean imaKomentarje = false;
                while (rs.next()) {
                    imaKomentarje = true;
                    int ocena = rs.getInt("ocena");
                    String komentar = rs.getString("komentar");
                    String username = rs.getString("username");

                    sb.append("\nUporabnik: ").append(username)
                            .append("\nOcena: ").append(ocena)
                            .append("\nKomentar: ").append(komentar == null ? "" : komentar)
                            .append("\n----------------------------\n");
                }

                if (!imaKomentarje) {
                    sb.append("Ni še nobenih komentarjev.");
                }

                komentarArea.setText(sb.toString());

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Napaka pri nalaganju komentarjev ali storitev: " + e.getMessage());
            }

            setLocationRelativeTo(parent);
            setVisible(true);
        }
    }
}


