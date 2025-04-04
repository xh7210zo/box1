package edu.uob;

import org.junit.jupiter.api.Test;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.util.Iterator;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import com.alexmerz.graphviz.objects.Edge;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntitiesFileTests {

    // Test to make sure that the basic entities file is readable
    @Test
    void testBasicEntitiesFileIsReadable() {
        try {
            Parser parser = new Parser();
            StringBuilder filePath = new StringBuilder();
            filePath.append("config").append(File.separator).append("extended-entities.dot");
            FileReader reader = new FileReader(filePath.toString());

            parser.parse(reader);
            Graph wholeDocument = parser.getGraphs().get(0);
            Iterator<Graph> sections = wholeDocument.getSubgraphs().iterator();

            // The locations will always be in the first subgraph
            Graph firstLocation = sections.next().getSubgraphs().iterator().next();
            Node locationDetails = firstLocation.getNodes(false).iterator().next();
            // Yes, you do need to get the ID twice !
            String locationName = locationDetails.getId().getId();
            assertEquals("cabin", locationName, "First location should have been 'cabin'");

            // The paths will always be in the second subgraph
            Iterator<Edge> paths = sections.next().getEdges().iterator();
            Edge firstPath = paths.next();
            Node fromLocation = firstPath.getSource().getNode();
            String fromName = fromLocation.getId().getId();
            Node toLocation = firstPath.getTarget().getNode();
            String toName = toLocation.getId().getId();
            assertEquals("cabin", fromName, "First path should have been from 'cabin'");
            assertEquals("forest", toName, "First path should have been to 'forest'");

        } catch (FileNotFoundException fnfe) {
            fail("FileNotFoundException was thrown when attempting to read basic entities file");
        } catch (ParseException pe) {
            fail("ParseException was thrown when attempting to read basic entities file");
        }
    }

}
