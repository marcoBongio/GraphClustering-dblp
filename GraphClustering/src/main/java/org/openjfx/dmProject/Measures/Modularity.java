package org.openjfx.dmProject.Measures;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Graph.GraphDB;

import java.util.List;
import java.util.Map;

public class Modularity {
    public static double[] calculateEdges(GraphDB clusterDB, Map.Entry<Integer, List<String>> cluster){
        double[] edges = new double[2];
        int clusterEdges = 0;
        int degreeSum = 0;

        try (Transaction tx = clusterDB.beginTx()) {
            String query = "MATCH (p:Person)-[r:COAUTHOR]->(p2:Person) WHERE p.cluster = " + cluster.getKey() + " AND p2.cluster = " + cluster.getKey() + " RETURN count(r) AS count";
            Result result = tx.execute(query);
            Map<String, Object> row = result.next();
            clusterEdges = Math.toIntExact((Long) row.get("count"));

            String query2 = "MATCH (p:Person)-[r:COAUTHOR]->(p2:Person) WHERE p.cluster = " + cluster.getKey() + " RETURN count(r) AS count";
            Result result2 = tx.execute(query2);
            Map<String, Object> row2 = result2.next();
            degreeSum = Math.toIntExact((Long) row2.get("count"));
        } catch (Error ignored) {}
        edges[0] = clusterEdges;
        edges[1] = degreeSum;

        return edges;
    }

    public static Double calculateModularity(GraphDB clusterDB, Map<Integer, List<String>> clusters){
        double mod = 0.0;
        double[] edges;
        double score1;
        double score2;
        for (Map.Entry<Integer, List<String>> cluster : clusters.entrySet()){
            edges = calculateEdges(clusterDB, cluster);
            //ls/L => number of edges in cluster / total number of edges in graph
            score1 = edges[0]/clusterDB.totEdges;
            //(ds/2L)^2 => (sum of the degree of nodes in the cluster / total number of edges in graph * 2)^2
            score2 = Math.pow((edges[1]/(2.0*clusterDB.totEdges)), 2);
            mod = mod + score1 - score2;
        }
        return mod;
    }
}
