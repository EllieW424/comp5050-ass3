import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.Test;

import nz.sodium.StreamSink;
import swidgets.SButton;
import swidgets.STextArea;
import swidgets.STextField;

public class GpsGUILayout_Test {
    @Test
    public void laysOutEveryRequiredPanelTableAndFilterControl() throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);
        JPanel content = createOnEventThread(positions);

        assertNotNull(findByName(content, "tracker-displays-section"));
        assertNotNull(findByName(content, "most-recent-event-section"));
        assertNotNull(findByName(content, "filter-section"));
        assertNotNull(findByName(content, "filtered-event-section"));
        assertNotNull(findByName(content, "distance-section"));

        JTable trackerTable = (JTable) findByName(content, "tracker-table");
        JTable distanceTable = (JTable) findByName(content, "distance-table");
        assertEquals(10, trackerTable.getRowCount());
        assertEquals(3, trackerTable.getColumnCount());
        assertEquals("Tracker", trackerTable.getColumnName(0));
        assertEquals("Latitude", trackerTable.getColumnName(1));
        assertEquals("Longitude", trackerTable.getColumnName(2));
        assertEquals(10, distanceTable.getRowCount());

        assertNotNull(findByName(content, "latitude-minimum-input"));
        assertNotNull(findByName(content, "latitude-maximum-input"));
        assertNotNull(findByName(content, "longitude-minimum-input"));
        assertNotNull(findByName(content, "longitude-maximum-input"));
        assertNotNull(findByName(content, "set-latitude-bounds"));
        assertNotNull(findByName(content, "set-longitude-bounds"));
        assertNotNull(findByName(content, "latitude-current-range"));
        assertNotNull(findByName(content, "longitude-current-range"));
        assertNotNull(findByName(content, "most-recent-event-text"));
        assertNotNull(findByName(content, "filtered-event-text"));

        assertEquals(4, findAll(content, STextField.class).size());
        assertEquals(2, findAll(content, SButton.class).size());
        assertEquals(2, findAll(content, STextArea.class).size());
    }

    @Test
    public void trackerTableReflectsCellUpdatesWithoutBlockingTheSendingThread() throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        SimplifiedGpsStreams positions = new SimplifiedGpsStreams(events);
        JPanel content = createOnEventThread(positions);
        JTable trackerTable = (JTable) findByName(content, "tracker-table");

        events.send(ReceivedGpsEvent.from(
                new GpsEvent("Tracker6", -12.3456789, -77.2500001, 600.0), 1L));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("-12.3456789", trackerTable.getValueAt(6, 1));
        assertEquals("-77.2500001", trackerTable.getValueAt(6, 2));
        assertEquals("-", trackerTable.getValueAt(5, 1));
    }

    @Test
    public void distanceTableTracksFilteredDistanceCellsForTheMatchingTracker()
            throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        JPanel content = createOnEventThread(new SimplifiedGpsStreams(events), events);
        JTable distanceTable = (JTable) findByName(content, "distance-table");
        STextField minimumLatitude = (STextField) findByName(content,
                "latitude-minimum-input");
        STextField maximumLatitude = (STextField) findByName(content,
                "latitude-maximum-input");
        SButton setLatitude = (SButton) findByName(content, "set-latitude-bounds");

        assertEquals("Tracker", distanceTable.getColumnName(0));
        assertEquals("Distance (m)", distanceTable.getColumnName(1));
        assertEquals("Tracker7", distanceTable.getValueAt(7, 0));
        assertEquals("No data", distanceTable.getValueAt(7, 1));

        setLatitudeBounds(minimumLatitude, maximumLatitude, setLatitude, "10", "20");
        sendEvent(events, "Tracker7", 25.0, 42.0, 1000.0);
        assertEquals("No data", distanceTable.getValueAt(7, 1));

        sendEvent(events, "Tracker7", 15.0, 42.0, 100.0);
        assertEquals("0", distanceTable.getValueAt(7, 1));
        assertEquals("No data", distanceTable.getValueAt(6, 1));

        sendEvent(events, "Tracker7", 15.0, 42.0, 101.0);
        assertEquals("1", distanceTable.getValueAt(7, 1));

        for (int index = 0; index < 64; index++) {
            events.send(ReceivedGpsEvent.from(
                    new GpsEvent("Tracker7", 15.0, 42.0, 102.0 + index), 0L));
        }
        flushEventThread();
        assertTrue(Long.parseLong(distanceTable.getValueAt(7, 1).toString()) > 1L);
        assertEquals("No data", distanceTable.getValueAt(6, 1));
    }

    @Test
    public void filterButtonsUpdateRangeLabelsAndShowRecoverableValidationErrors()
            throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        JPanel content = createOnEventThread(new SimplifiedGpsStreams(events));
        STextField minimumLatitude = (STextField) findByName(content,
                "latitude-minimum-input");
        STextField maximumLatitude = (STextField) findByName(content,
                "latitude-maximum-input");
        SButton setLatitude = (SButton) findByName(content, "set-latitude-bounds");
        JLabel currentLatitude = (JLabel) findByName(content, "latitude-current-range");
        JLabel latitudeError = (JLabel) findByName(content, "latitude-filter-error");

        SwingUtilities.invokeAndWait(() -> {
            minimumLatitude.setText("10");
            maximumLatitude.setText("20");
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(setLatitude::doClick);
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("Latitude range: 10.0 to 20.0", currentLatitude.getText());
        assertEquals("", latitudeError.getText().trim());
        assertEquals("Between -90 and 90", minimumLatitude.getAccessibleContext()
                .getAccessibleDescription());

        SwingUtilities.invokeAndWait(() -> {
            minimumLatitude.setText("30");
            maximumLatitude.setText("20");
        });
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(setLatitude::doClick);
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("Latitude range: 10.0 to 20.0", currentLatitude.getText());
        assertTrue(latitudeError.getText().contains("Min no greater than Max"));
        assertEquals(latitudeError.getText(), minimumLatitude.getAccessibleContext()
                .getAccessibleDescription());
    }

    @Test
    public void filteredEventDisplayTracksOnlyEventsInsideTheCurrentBounds() throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        JPanel content = createOnEventThread(new SimplifiedGpsStreams(events), events);
        STextField minimumLatitude = (STextField) findByName(content,
                "latitude-minimum-input");
        STextField maximumLatitude = (STextField) findByName(content,
                "latitude-maximum-input");
        SButton setLatitude = (SButton) findByName(content, "set-latitude-bounds");
        STextArea mostRecent = (STextArea) findByName(content, "most-recent-event-text");
        STextArea filtered = (STextArea) findByName(content, "filtered-event-text");

        setLatitudeBounds(minimumLatitude, maximumLatitude, setLatitude, "10", "20");
        sendEvent(events, "Inside", 15.0, 42.0);
        assertEquals("Inside,15.0,42.0,100.0", mostRecent.getText());
        assertEquals(mostRecent.getText(), filtered.getText());

        sendEvent(events, "Outside", 21.0, 42.0);
        assertEquals("Outside,21.0,42.0,100.0", mostRecent.getText());
        assertEquals("Inside,15.0,42.0,100.0", filtered.getText());

        setLatitudeBounds(minimumLatitude, maximumLatitude, setLatitude, "20", "30");
        sendEvent(events, "NewRange", 25.0, 42.0);
        assertEquals("NewRange,25.0,42.0,100.0", mostRecent.getText());
        assertEquals(mostRecent.getText(), filtered.getText());
    }

    @Test
    public void sectionLayoutRemainsDistinctAtMinimumAndDesktopSizes() throws Exception {
        StreamSink<ReceivedGpsEvent> events = new StreamSink<>();
        JPanel content = createOnEventThread(new SimplifiedGpsStreams(events));
        JScrollPane scrollPane = (JScrollPane) findByName(content, "dashboard-scroll-pane");
        JPanel dashboard = (JPanel) scrollPane.getViewport().getView();

        assertSectionsDoNotOverlap(content, dashboard, new Dimension(1200, 900));
        assertSectionsDoNotOverlap(content, dashboard, new Dimension(960, 680));
    }

    private static JPanel createOnEventThread(SimplifiedGpsStreams positions)
            throws InterruptedException, InvocationTargetException {
        AtomicReference<JPanel> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(GpsGUI.createContent(positions)));
        return result.get();
    }

    private static JPanel createOnEventThread(SimplifiedGpsStreams positions,
                                              StreamSink<ReceivedGpsEvent> events)
            throws InterruptedException, InvocationTargetException {
        AtomicReference<JPanel> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> result.set(GpsGUI.createContent(positions, events)));
        return result.get();
    }

    private static void setLatitudeBounds(STextField minimum, STextField maximum,
                                          SButton set, String lower, String upper)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            minimum.setText(lower);
            maximum.setText(upper);
        });
        SwingUtilities.invokeAndWait(set::doClick);
        flushEventThread();
    }

    private static void sendEvent(StreamSink<ReceivedGpsEvent> events, String tracker,
                                  double latitude, double longitude)
            throws InterruptedException, InvocationTargetException {
        sendEvent(events, tracker, latitude, longitude, 100.0);
    }

    private static void sendEvent(StreamSink<ReceivedGpsEvent> events, String tracker,
                                  double latitude, double longitude, double altitudeFeet)
            throws InterruptedException, InvocationTargetException {
        events.send(ReceivedGpsEvent.from(
                new GpsEvent(tracker, latitude, longitude, altitudeFeet), 0L));
        flushEventThread();
    }

    private static void flushEventThread()
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void assertSectionsDoNotOverlap(JPanel content, JPanel dashboard,
                                                   Dimension size)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {
            content.setSize(size);
            layoutRecursively(content);
        });

        List<Component> sections = new ArrayList<>();
        for (String name : new String[] {
                "tracker-displays-section", "most-recent-event-section", "filter-section",
                "filtered-event-section", "distance-section" }) {
            Component section = findByName(dashboard, name);
            assertNotNull(section);
            assertTrue("Empty section at " + name + " for " + size,
                    section.getWidth() > 0 && section.getHeight() > 0);
            sections.add(section);
        }

        for (int first = 0; first < sections.size(); first++) {
            Rectangle firstBounds = sections.get(first).getBounds();
            for (int second = first + 1; second < sections.size(); second++) {
                assertFalse("Sections overlap at " + size,
                        firstBounds.intersects(sections.get(second).getBounds()));
            }
        }
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layoutRecursively((Container) child);
            }
        }
    }

    private static Component findByName(Container root, String name) {
        if (name.equals(root.getName())) {
            return root;
        }
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container) {
                Component found = findByName((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) {
            result.add(type.cast(root));
        }
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                result.add(type.cast(child));
            } else if (child instanceof Container) {
                result.addAll(findAll((Container) child, type));
            }
        }
        return result;
    }
}
