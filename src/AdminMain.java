import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class AdminMain extends JFrame {
    private JTable domoviTable;

    public AdminMain() {
        setTitle("Admin - Upravljanje domov");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Gumbi na vrhu
        JPanel topPanel = new JPanel(new FlowLayout());
        JButton addHomeBtn = new JButton("Dodaj dom");
        JButton editHomeBtn = new JButton("Uredi dom");
        JButton deleteHomeBtn = new JButton("Izbriši dom");
        JButton manageServicesBtn = new JButton("Upravljanje storitev");
        JButton pregledOcenBtn = new JButton("Pregled ocen"); // TA GUMB

        // Dodaj gumbe v panel
        topPanel.add(addHomeBtn);
        topPanel.add(editHomeBtn);
        topPanel.add(deleteHomeBtn);
        topPanel.add(manageServicesBtn);
        topPanel.add(pregledOcenBtn); // DODANO TUKAJ

        // Tabela domov
        domoviTable = new JTable();
        JScrollPane tableScroll = new JScrollPane(domoviTable);

        // Dodaj v glavni layout
        add(topPanel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);

        // Dogodki
        addHomeBtn.addActionListener(e -> new DodajDomDialog(this));
        editHomeBtn.addActionListener(e -> {
            int row = domoviTable.getSelectedRow();
            if (row >= 0) {
                int domId = (int) domoviTable.getValueAt(row, 0);
                new UrediDomDialog(this, domId);
            } else {
                JOptionPane.showMessageDialog(this, "Najprej izberi dom.");
            }
        });

        deleteHomeBtn.addActionListener(e -> {
            int selectedRow = domoviTable.getSelectedRow();
            if (selectedRow != -1) {
                int homeId = (int) domoviTable.getValueAt(selectedRow, 0);
                int confirmed = JOptionPane.showConfirmDialog(this,
                        "Ste prepričani, da želite izbrisati ta dom?",
                        "Potrdite izbris", JOptionPane.YES_NO_OPTION);
                if (confirmed == JOptionPane.YES_OPTION) {
                    deleteHome(homeId);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Prosimo, izberite dom za brisanje.");
            }
        });

        manageServicesBtn.addActionListener(e -> new StoritveDialog());

        pregledOcenBtn.addActionListener(e -> new PregledOcenDialog(this)); // DOGODEK ZA NOV GUMB

        loadHomes();
        setVisible(true);
    }

    public void loadHomes() {
        try (Connection conn = DBConnection.connect()) {
            String sql = "SELECT d.id, d.ime, d.naslov, k.ime AS kraj FROM domovi d JOIN kraji k ON d.kraj_id = k.id";
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

    private void deleteHome(int homeId) {
        try (Connection conn = DBConnection.connect()) {
            String sql = "DELETE FROM domovi WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, homeId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Dom je bil uspešno izbrisan.");
            loadHomes();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Napaka pri brisanju doma: " + e.getMessage());
        }
    }

    // ================== STORITVE DIALOG ===================
    class StoritveDialog extends JDialog {
        private JTable storitveTable;

        public StoritveDialog() {
            setTitle("Upravljanje storitev");
            setSize(500, 400);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            storitveTable = new JTable();
            JScrollPane scrollPane = new JScrollPane(storitveTable);

            JPanel buttonPanel = new JPanel();
            JButton addBtn = new JButton("Dodaj storitev");
            JButton deleteBtn = new JButton("Izbriši storitev");
            buttonPanel.add(addBtn);
            buttonPanel.add(deleteBtn);

            add(scrollPane, BorderLayout.CENTER);
            add(buttonPanel, BorderLayout.SOUTH);

            // Dogodki
            addBtn.addActionListener(e -> {
                String ime = JOptionPane.showInputDialog(this, "Vnesite ime storitve:");
                if (ime != null && !ime.trim().isEmpty()) {
                    try (Connection conn = DBConnection.connect()) {
                        String sql = "INSERT INTO storitve (ime) VALUES (?)";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, ime);
                        ps.executeUpdate();
                        loadServices();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Napaka pri dodajanju: " + ex.getMessage());
                    }
                }
            });

            deleteBtn.addActionListener(e -> {
                int row = storitveTable.getSelectedRow();
                if (row >= 0) {
                    int id = (int) storitveTable.getValueAt(row, 0);
                    int confirm = JOptionPane.showConfirmDialog(this, "Ste prepričani?", "Potrditev", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try (Connection conn = DBConnection.connect()) {
                            String sql = "DELETE FROM storitve WHERE id = ?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setInt(1, id);
                            ps.executeUpdate();
                            loadServices();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Napaka pri brisanju: " + ex.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Najprej izberite storitev.");
                }
            });

            loadServices();
            setVisible(true);
        }

        private void loadServices() {
            try (Connection conn = DBConnection.connect()) {
                String sql = "SELECT id, ime FROM storitve";
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                Vector<String> columnNames = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnLabel(i));
                }

                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }
                storitveTable.setModel(new DefaultTableModel(data, columnNames));

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Napaka pri nalaganju storitev: " + e.getMessage());
            }
        }
    }
}
