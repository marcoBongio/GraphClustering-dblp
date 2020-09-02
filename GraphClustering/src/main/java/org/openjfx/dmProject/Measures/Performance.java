package org.openjfx.dmProject.Measures;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Graph.GraphDB;

import java.util.List;
import java.util.Map;

public class Performance {
    public static Double calculateClusteringPerformance(GraphDB clusterDB, Map<Integer, List<String>> clusters){
        double perf;
        String query = "MATCH (p1:Person)-[c:COAUTHOR]->(p2:Person) WHERE p1.cluster = p2.cluster AND p1.cluster >= 0 RETURN count(c) as count";
        double clusterEdges = 0.0;
        double score; //m * (1 - 2 * (m(C) / m))
        double score2 = 0.0; // Ʃ |Ci| (|Ci| - 1)
        int score3 = clusterDB.personCount * (clusterDB.personCount - 1); //n * (n - 1)

        try (Transaction tx = clusterDB.beginTx();
             Result result = tx.execute(query)) {
            Map<String, Object> row = result.next();
            clusterEdges = (Long) row.get("count");

            tx.commit();
        } catch (Error ignored) {}

        score = clusterDB.totEdges * (1 - 2 * clusterEdges/clusterDB.totEdges);

        for (Map.Entry<Integer, List<String>> r : clusters.entrySet()){
            List<String> list = r.getValue();
            int clusterNodes = list.size();
            score2 += clusterNodes * (clusterNodes - 1);
        }

        perf = 1 - ((score + score2)/score3);
        //System.out.println(score + " " + score2 + " " + score3 + " " + perf);
        return perf;
    }

}
