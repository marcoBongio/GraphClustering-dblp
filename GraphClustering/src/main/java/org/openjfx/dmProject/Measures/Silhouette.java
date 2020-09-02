package org.openjfx.dmProject.Measures;

import org.neo4j.graphalgo.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Clustering.ClusterRelTypes;
import org.openjfx.dmProject.Graph.GraphDB;
import org.openjfx.dmProject.Graph.NodeType;
import org.openjfx.dmProject.Graph.Utils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Silhouette {
    private static Double[][] distances;

    public static void createDistanceMatrix(GraphDB clusterGraph, List<String> nodes, String metapath, Double eps, Integer mu) throws IOException {
        distances = new Double[clusterGraph.personCount][clusterGraph.personCount];
        String pathFile = "src/main/java/org/openjfx/dmProject/Measures/DistanceMatrix" + metapath + "eps" + eps + "mu" + mu + ".csv";
        File file = new File(pathFile);
        FileWriter csvWriter = new FileWriter(file, true);

        for (Double[] row : distances)
            Arrays.fill(row, -1.0);

        List<String[]> silhouettes = Utils.parseFile(pathFile, "\\|");
        for( String[] item : silhouettes){
            distances[Integer.parseInt(item[0])][Integer.parseInt(item[1])] = Double.parseDouble(item[2]);
            distances[Integer.parseInt(item[1])][Integer.parseInt(item[0])] = Double.parseDouble(item[2]);
        }

        for (String node : nodes) {
            try (Transaction tx = clusterGraph.beginTx()) {
                Node n1 = tx.findNode(NodeType.Person, "id", node);
                System.out.println(node);
                PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(new BasicEvaluationContext(tx, clusterGraph.graphDB),
                        PathExpanders.forTypeAndDirection(ClusterRelTypes.COAUTHOR, Direction.OUTGOING), "distance");

                for (String node2 : nodes) {
                    if (distances[Integer.parseInt(node)][Integer.parseInt(node2)] == -1.0) {
                        //System.out.println("\t" + node2);
                        Node n2 = tx.findNode(NodeType.Person, "id", node2);

                        WeightedPath path = finder.findSinglePath(n1, n2);
                        System.out.println(path.toString());
                        double weight = path.weight();

                        distances[Integer.parseInt(node)][Integer.parseInt(node2)] = weight;
                        distances[Integer.parseInt(node2)][Integer.parseInt(node)] = weight;
                        csvWriter.append(node);
                        csvWriter.append("|");
                        csvWriter.append(node2);
                        csvWriter.append("|");
                        csvWriter.append(Double.toString(weight));
                        csvWriter.append("\n");
                        //System.out.print("[" + id + ":" + weight+"]");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        csvWriter.flush();
        csvWriter.close();
        System.out.println("Distance Matrix Created");
    }

    public static Double calculateNodeSilhouette(String nodeID, Map<Integer, List<String>> clusters, Integer clusterID, List<String> clusterNodes) {
        double sil;
        int nodeIndex = Integer.parseInt(nodeID);

        int countCluster = 0, countNear;
        double avgCluster = 0.0, avg, avgFinal = 100.0;

        if(clusterNodes != null){
             if(clusterNodes.size() == 1) {
                 sil = 0.0;
                 return sil;
             }

            for (String index : clusterNodes) {
                int i = Integer.parseInt(index);
                if (i != nodeIndex && distances[nodeIndex][i] != -1.0) {
                    avgCluster += distances[nodeIndex][i];
                    countCluster++;
                }
            }
        }

        avgCluster = avgCluster/countCluster;

        for (Map.Entry<Integer, List<String>> r : clusters.entrySet()){
            if(!r.getKey().equals(clusterID)) {
                List<String> list = r.getValue();
                countNear = 0;
                avg = 0.0;
                for (String s : list) {
                    int id = Integer.parseInt(s);
                    if (distances[nodeIndex][id] != -1.0) {
                        avg += distances[nodeIndex][id];
                        countNear++;
                    }
                }
                avg = avg / countNear;
                if (avg <= avgFinal)
                    avgFinal = avg;
            }
        }

        double max = Math.max(avgCluster, avgFinal);

        sil = (avgFinal - avgCluster)/max;
        //System.out.println("Node " + nodeID +": " + avgCluster + ", " + avgFinal + ", " + sil);
        return sil;
    }

    public static Double calculateClusterSilhouette(Map<Integer, List<String>> clusters, Map.Entry<Integer, List<String>> c){
        double silC = 0.0;
        Integer clusterID = c.getKey();
        List<String> clusterNodes = c.getValue();

        if(clusterNodes.size() == 0)
            return silC;

        for (String nodeID : clusterNodes) {
            silC += calculateNodeSilhouette(nodeID, clusters, clusterID, clusterNodes);
        }

        silC = silC/clusterNodes.size();
        //System.out.println("Cluster " + clusterID +": " + silC);
        return silC;
    }

    public static Double calculateGraphSilhouette(GraphDB clusterGraph, Map<Integer, List<String>> clusters, String metapath, Double eps, Integer mu) throws IOException {
        double silG = 0.0;
        List<String> nodes = Utils.findAllPerson(clusterGraph);
        createDistanceMatrix(clusterGraph, nodes, metapath, eps, mu);

        for (Map.Entry<Integer, List<String>> c : clusters.entrySet()){
            silG += calculateClusterSilhouette(clusters, c);
         }

        silG = silG/clusters.size();

        return silG;
    }
}

