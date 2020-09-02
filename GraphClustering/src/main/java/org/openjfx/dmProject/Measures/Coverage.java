package org.openjfx.dmProject.Measures;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Graph.GraphDB;

import java.util.Map;

public class Coverage {

    public static Double calculateCoverage(GraphDB clusterDB) {
        Double cov;
        Double totClusteredEdges;
        Double totEdges;

        String query = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) WHERE p1.cluster = p2.cluster AND p1.cluster >= 0 RETURN sum(c.similarity) as clusteredEdges";
        try (Transaction tx = clusterDB.beginTx();
             Result result = tx.execute(query)) {
            Map<String, Object> row = result.next();
            totClusteredEdges = (Double) row.get("clusteredEdges");
        }

        String query2 = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) RETURN sum(c.similarity) as totEdges";
        try (Transaction tx = clusterDB.beginTx();
             Result result2 = tx.execute(query2)) {
            Map<String, Object> row2 = result2.next();
            totEdges = (Double) row2.get("totEdges");
        }

        cov = totClusteredEdges/totEdges;

        return  cov;
    }
}
