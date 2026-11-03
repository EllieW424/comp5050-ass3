import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import nz.sodium.Cell;
import nz.sodium.Listener;
import nz.sodium.Operational;
import swidgets.SButton;
import swidgets.STextArea;
import swidgets.STextField;

/** Desktop layout for the course GPS tracker monitor. */
public final class GpsGUI {
    private static final Color BACKGROUND = new Color(244, 247, 244);
    private static final Color SURFACE = Color.WHITE;
    private static final Color TEXT = new Color(31, 43, 40);
    private static final Color MUTED = new Color(83, 99, 94);
    private static final Color BORDER = new Color(211, 221, 216);
    private static final Color ACCENT = new Color(0, 91, 84);
    private static final Color TABLE_HEADER = new Color(232, 239, 235);

    private final SimplifiedGpsStreams positions;

    private GpsGUI(GpsServiceAdapter adapter) {
        this.positions = new SimplifiedGpsStreams(adapter.events());
    }

    public static void main(String[] args) {
        GpsServiceAdapter adapter = new GpsServiceAdapter();
        SwingUtilities.invokeLater(() -> new GpsGUI(adapter).showWindow());
    }

    static JPanel createContent(SimplifiedGpsStreams positions) {
        JPanel shell = new JPanel(new BorderLayout(0, 14));
        shell.setName("gps-dashboard");
        shell.setBackground(BACKGROUND);
        shell.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        shell.add(createHeader(), BorderLayout.NORTH);

        JPanel dashboard = new JPanel(new GridBagLayout());
        dashboard.setBackground(BACKGROUND);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 12, 12);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.6;
        constraints.weighty = 0.58;
        dashboard.add(createTrackerSection(positions), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.4;
        constraints.insets = new Insets(0, 0, 12, 0);
        dashboard.add(createFilterSection(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 0.21;
        constraints.insets = new Insets(0, 0, 12, 12);
        dashboard.add(createEventSection("Most Recent Event", "most-recent-event"), constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridheight = 2;
        constraints.weightx = 0.4;
        constraints.weighty = 0.42;
        constraints.insets = new Insets(0, 0, 12, 0);
        dashboard.add(createDistanceSection(), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridheight = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 0.21;
        constraints.insets = new Insets(0, 0, 0, 12);
        dashboard.add(createEventSection("Filtered Event", "filtered-event"), constraints);

        JScrollPane viewport = new JScrollPane(dashboard,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        viewport.setName("dashboard-scroll-pane");
        viewport.setBorder(BorderFactory.createEmptyBorder());
        viewport.getViewport().setBackground(BACKGROUND);
        shell.add(viewport, BorderLayout.CENTER);
        return shell;
    }

    private void showWindow() {
        JFrame frame = new JFrame("GPS Tracker Monitor");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 680));
        frame.setContentPane(createContent(positions));
        frame.setSize(1200, 900);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    private static JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 3));
        header.setOpaque(false);

        JLabel title = new JLabel("GPS Tracker Monitor");
        title.setName("dashboard-title");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        header.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Course GPS feed overview");
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(13f));
        header.add(subtitle, BorderLayout.CENTER);
        return header;
    }

    private static JPanel createTrackerSection(SimplifiedGpsStreams positions) {
        JPanel section = createSection("Tracker Displays", "tracker-displays-section");
        JTable table = new JTable(new TrackerTableModel(positions));
        table.setName("tracker-table");
        table.getAccessibleContext().setAccessibleName("Tracker Displays");
        table.getAccessibleContext().setAccessibleDescription(
                "Ten tracker rows with latest latitude and longitude; altitude is omitted.");
        styleTable(table);
        table.setRowHeight(31);
        table.setPreferredScrollableViewportSize(new Dimension(500, 341));
        table.getColumnModel().getColumn(0).setPreferredWidth(125);
        table.getColumnModel().getColumn(1).setPreferredWidth(185);
        table.getColumnModel().getColumn(2).setPreferredWidth(185);
        table.getColumnModel().getColumn(1).setCellRenderer(coordinateRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(coordinateRenderer());
        section.add(tableScrollPane(table), BorderLayout.CENTER);
        return section;
    }

    private static JPanel createFilterSection() {
        JPanel section = createSection("Latitude/Longitude Filter", "filter-section");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        addFilterRow(form, 0, "Latitude", "latitude", "-90 to 90");
        addFilterRow(form, 2, "Longitude", "longitude", "-180 to 180");
        section.add(form, BorderLayout.CENTER);
        return section;
    }

    private static void addFilterRow(JPanel form, int row, String axis, String id,
                                     String initialRange) {
        STextField minimum = new STextField("", 7);
        minimum.setName(id + "-minimum-input");
        minimum.getAccessibleContext().setAccessibleName("Minimum " + axis.toLowerCase());

        STextField maximum = new STextField("", 7);
        maximum.setName(id + "-maximum-input");
        maximum.getAccessibleContext().setAccessibleName("Maximum " + axis.toLowerCase());

        SButton set = new SButton("Set", new Cell<>(false));
        set.setName("set-" + id + "-bounds");
        set.setToolTipText("Set " + axis.toLowerCase() + " bounds");
        set.getAccessibleContext().setAccessibleName("Set " + axis.toLowerCase() + " bounds");

        JLabel axisLabel = new JLabel(axis);
        axisLabel.setForeground(TEXT);
        axisLabel.setFont(axisLabel.getFont().deriveFont(Font.BOLD, 13f));
        axisLabel.setLabelFor(minimum);

        JLabel minLabel = new JLabel("Min");
        minLabel.setForeground(MUTED);
        minLabel.setLabelFor(minimum);
        JLabel maxLabel = new JLabel("Max");
        maxLabel.setForeground(MUTED);
        maxLabel.setLabelFor(maximum);

        addFormComponent(form, axisLabel, 0, row, 0.0);
        addFormComponent(form, minLabel, 1, row, 0.0);
        addFormComponent(form, minimum, 2, row, 0.3);
        addFormComponent(form, maxLabel, 3, row, 0.0);
        addFormComponent(form, maximum, 4, row, 0.3);
        addFormComponent(form, set, 5, row, 0.0);

        JLabel current = new JLabel("Current " + axis.toLowerCase() + " range: " + initialRange);
        current.setName(id + "-current-range");
        current.setForeground(MUTED);
        current.setFont(current.getFont().deriveFont(12f));
        GridBagConstraints currentConstraints = new GridBagConstraints();
        currentConstraints.gridx = 1;
        currentConstraints.gridy = row + 1;
        currentConstraints.gridwidth = 5;
        currentConstraints.anchor = GridBagConstraints.WEST;
        currentConstraints.fill = GridBagConstraints.HORIZONTAL;
        currentConstraints.weightx = 1.0;
        currentConstraints.insets = new Insets(0, 4, 9, 2);
        form.add(current, currentConstraints);
    }

    private static void addFormComponent(JPanel form, java.awt.Component component,
                                         int column, int row, double weightx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = weightx;
        constraints.fill = column == 2 || column == 4
                ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 4, 2, 4);
        form.add(component, constraints);
    }

    private static JPanel createEventSection(String title, String name) {
        JPanel section = createSection(title, name + "-section");
        STextArea event = new STextArea("Waiting for GPS data", 2, 30);
        event.setName(name + "-text");
        event.setEditable(false);
        event.setLineWrap(true);
        event.setWrapStyleWord(true);
        event.setForeground(TEXT);
        event.setBackground(new Color(249, 251, 249));
        event.setFont(event.getFont().deriveFont(14f));
        event.getAccessibleContext().setAccessibleName(title);
        section.add(event, BorderLayout.CENTER);
        return section;
    }

    private static JPanel createDistanceSection() {
        JPanel section = createSection("Distance by Tracker", "distance-section");
        Object[][] rows = new Object[SimplifiedGpsStreams.TRACKER_COUNT][2];
        for (int row = 0; row < rows.length; row++) {
            rows[row][0] = "Tracker" + row;
            rows[row][1] = "-";
        }
        DefaultTableModel model = new DefaultTableModel(
                rows, new Object[] { "Tracker", "Distance (m)" }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setName("distance-table");
        table.getAccessibleContext().setAccessibleName("Distance by Tracker");
        styleTable(table);
        table.setRowHeight(29);
        table.setPreferredScrollableViewportSize(new Dimension(360, 319));
        table.getColumnModel().getColumn(1).setCellRenderer(coordinateRenderer());
        section.add(tableScrollPane(table), BorderLayout.CENTER);
        return section;
    }

    private static JPanel createSection(String title, String name) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setName(name);
        section.setBackground(SURFACE);
        TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title,
                TitledBorder.LEADING, TitledBorder.TOP,
                UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f), ACCENT);
        section.setBorder(BorderFactory.createCompoundBorder(titled,
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        return section;
    }

    private static JScrollPane tableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
        return scrollPane;
    }

    private static void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setColumnSelectionAllowed(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(TABLE_HEADER);
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(table.getTableHeader().getFont()
                .deriveFont(Font.BOLD, 12f));
        table.setFont(table.getFont().deriveFont(13f));
    }

    private static DefaultTableCellRenderer coordinateRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        renderer.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        renderer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 10));
        return renderer;
    }

    private static final class TrackerTableModel extends AbstractTableModel {
        private final List<String> trackers;
        private final List<Cell<Optional<SimplifiedGps>>> positions;
        private final List<Listener> listeners = new ArrayList<>();

        private TrackerTableModel(SimplifiedGpsStreams streams) {
            this.trackers = streams.getTrackerNames();
            this.positions = new ArrayList<>(trackers.size());
            for (int row = 0; row < trackers.size(); row++) {
                Cell<Optional<SimplifiedGps>> position = streams.getPosition(trackers.get(row));
                positions.add(position);
                final int updatedRow = row;
                listeners.add(Operational.updates(position).listen(
                        value -> SwingUtilities.invokeLater(
                                () -> fireTableRowsUpdated(updatedRow, updatedRow))));
            }
        }

        @Override
        public int getRowCount() {
            return trackers.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Tracker";
                case 1:
                    return "Latitude";
                case 2:
                    return "Longitude";
                default:
                    throw new IndexOutOfBoundsException("Unknown tracker column: " + column);
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return trackers.get(row);
            }
            Optional<SimplifiedGps> value = positions.get(row).sample();
            if (!value.isPresent()) {
                return "-";
            }
            SimplifiedGps point = value.get();
            if (column == 1) {
                return Double.toString(point.getLatitude());
            }
            if (column == 2) {
                return Double.toString(point.getLongitude());
            }
            throw new IndexOutOfBoundsException("Unknown tracker column: " + column);
        }
    }
}
