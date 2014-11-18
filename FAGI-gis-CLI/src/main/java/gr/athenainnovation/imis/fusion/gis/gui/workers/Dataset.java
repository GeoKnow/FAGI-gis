package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.*;

/**
 * Stores information about a dataset accessible via SPARQL endpoint.
 * @author Thomas Maroulis
 */
public class Dataset {
    private final String endpoint, graph, subjectRegex;

    /**
     * Constructs a new dataset with a given SPARQL endpoint, graph URI and regex for matching the URI's of a family of nodes.
     * @param endpoint SPARQL endpoint URL
     * @param graph graph URI
     * @param subjectRegex regex for matching node URIs
     */
    public Dataset(final String endpoint, final String graph, final String subjectRegex) {
        this.endpoint = checkNotNull(endpoint, "Endpoint cannot be null.");
        this.graph = checkNotNull(graph, "Graph cannot be null.");
        this.subjectRegex = checkNotNull(subjectRegex, "Subject regex cannot be null.");
    }

    /**
     *
     * @return SPARQL endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     *
     * @return graph URI
     */
    public String getGraph() {
        return graph;
    }

    /**
     *
     * @return regex for matching node URIs
     */
    public String getSubjectRegex() {
        return subjectRegex;
    }
}
