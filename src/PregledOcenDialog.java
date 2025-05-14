import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class PregledOcenDialog extends JDialog {
    private JTable table;
    private DefaultTableModel model;

    public PregledOcenDialog(JFrame parent) {
        super(parent, "Pregled ocen", true);
        setSize(800, 500);
        setLocationRelativeTo(parent);

        model = new DefaultTableModel(new Object[]{"Dom", "Povp. ocena", "Št. ocen", "Komentar", "Uporabnik", "Izbriši", "ID ocene"}, 0);
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Gumb za izbris
        JButton deleteBtn = new JButton("Izbriši oceno");
        deleteBtn.addActionListener(e -> deleteRating());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(deleteBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        loadData();

        setVisible(true);
    }

    private void loadData() {
        try (Connection conn = DBConnection.connect()) {
            String sql = """
    SELECT d.ime AS dom, 
           ROUND(AVG(o.ocena), 2) AS povprecje, 
           COUNT(o.id) AS st_glasov, 
           o.komentar AS komentar,
           u.username AS uporabnik,
           o.id AS ocena_id
    FROM domovi d
    LEFT JOIN ocene o ON d.id = o.dom_id
    LEFT JOIN uporabniki u ON o.uporabnik_id = u.id
    GROUP BY d.id, o.id, u.username
    ORDER BY d.ime
""";

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String dom = rs.getString("dom");
                double povprecje = rs.getDouble("povprecje");
                int stGlasov = rs.getInt("st_glasov");
                String komentar = rs.getString("komentar");
                String uporabnik = rs.getString("uporabnik");
                int ocenaId = rs.getInt("ocena_id");

                // Dodaj vse podatke v tabelo, vključno z ID-jem ocene
                model.addRow(new Object[]{dom, povprecje, stGlasov, komentar, uporabnik, "Izbriši", ocenaId});
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Napaka pri nalaganju ocen: " + e.getMessage());
        }
    }

    private void deleteRating() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int ratingId = (int) table.getValueAt(row, 6); // ID ocene je zdaj v zadnjem stolpcu (6. stolpec)
            int confirmed = JOptionPane.showConfirmDialog(this, "Ste prepričani, da želite izbrisati to oceno?", "Potrditev izbrisa", JOptionPane.YES_NO_OPTION);
            if (confirmed == JOptionPane.YES_OPTION) {
                try (Connection conn = DBConnection.connect()) {
                    String sql = "DELETE FROM ocene WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setInt(1, ratingId);
                    ps.executeUpdate();
                    model.removeRow(row); // Odstrani vrstico iz tabele
                    JOptionPane.showMessageDialog(this, "Ocena je bila uspešno odstranjena.");
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Napaka pri brisanju ocene: " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Izberite oceno, ki jo želite izbrisati.");
        }
    }
}
