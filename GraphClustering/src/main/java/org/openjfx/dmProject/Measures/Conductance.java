package org.openjfx.dmProject.Measures;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Graph.GraphDB;
import org.openjfx.dmProject.Graph.Utils;

import java.util.List;
import java.util.Map;

public class Conductance {

    public static Double calculateClusterConductance(GraphDB clusterDB, Integer clusterID) {
        Double condC;
        Object interClusterEdges;
        Object clusterEdges;
        Object totEdges;

        String query = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) WHERE p1.cluster = " + clusterID + " AND p2.cluster <> " + clusterID + " RETURN sum(c.similarity) as interClusterEdges";
        try (Transaction tx = clusterDB.beginTx();
             Result result = tx.execute(query)) {
            Map<String, Object> row = result.next();
            interClusterEdges = row.get("interClusterEdges");
        }

        String query2 = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) WHERE p1.cluster = " + clusterID + " RETURN sum(c.similarity) as clusterEdges";
        try (Transaction tx = clusterDB.beginTx();
             Result result2 = tx.execute(query2)) {
            Map<String, Object> row2 = result2.next();
            clusterEdges = row2.get("clusterEdges");
        }

        String query3 = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) RETURN sum(c.similarity) as totEdges";
        try (Transaction tx = clusterDB.beginTx();
             Result result3 = tx.execute(query3)) {
            Map<String, Object> row3 = result3.next();
            totEdges = row3.get("totEdges");
        }

        condC = Double.valueOf(Double.valueOf(interClusterEdges.toString()).doubleValue())/Math.min((Double.valueOf(clusterEdges.toString()).doubleValue()), (Double.valueOf(totEdges.toString()).doubleValue() - Double.valueOf(clusterEdges.toString()).doubleValue()));

        return  condC;
    }

    public static Double calculateConductance(GraphDB clusterDB, Map<Integer, List<String>> clusters) {
        double condG = 0.0;

        for (Map.Entry<Integer, List<String>> c : clusters.entrySet()){
            condG += calculateClusterConductance(clusterDB, c.getKey());
        }

        condG = condG/clusters.size();

        return condG;
    }
}
