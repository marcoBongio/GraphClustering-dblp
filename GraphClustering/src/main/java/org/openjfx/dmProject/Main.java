package org.openjfx.dmProject;

import org.openjfx.dmProject.Clustering.Scan;
import org.openjfx.dmProject.Graph.GraphDB;
import org.openjfx.dmProject.Graph.Utils;
import org.openjfx.dmProject.Measures.Conductance;
import org.openjfx.dmProject.Measures.Coverage;
import org.openjfx.dmProject.Measures.Modularity;
import org.openjfx.dmProject.Measures.Performance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static GraphDB graphDB;
    public static String dataPath = "target/databaseS/";
    public static String metapath = "APA";

    public static void main(String[] args) throws IOException, InterruptedException {

        graphDB = new GraphDB(dataPath);
        Scanner scanner = new Scanner(System.in);
        Double eps;
        Integer mu;
        Integer k;

        clearScreen();
        System.out.println("Insert metapath (path of similarity in the graph) parameter[APA, APAPA, APVPA]:");
        metapath = scanner.nextLine().trim();
        System.out.println(metapath);
        System.out.println("Insert mu (population threshold) parameter[min 2]:");
        mu = scanner.nextInt();
        System.out.println(mu);
        System.out.println("Insert k (number of neighbours) parameter[20, 30, 40]:");
        k = scanner.nextInt();
        System.out.println(k);
        System.out.println();

        int[] labels;
        Map<Integer, List<String>> clusters;

        try {
            graphDB.initializeGraphDB("src/main/resources/demo/");
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> all = Utils.findAllPerson(graphDB);

        Double[][] simMatrix;
        simMatrix = graphDB.createMatrix(metapath, all, k);
        System.out.println("Similarity Matrix " + metapath + k + "created!\n");
        eps = calculateEpsilon(simMatrix, graphDB.personCount);

        clearScreen();
        System.out.println("Scan Algorithm: " );
        System.out.println("Metapath = " + metapath );
        System.out.println("Similarity threshold = " + eps );
        System.out.println("Population threshold = " + mu );
        System.out.println("Number of neighbours = " + k );
        System.out.println();

        labels = Scan.clusterScan(graphDB, simMatrix, metapath, k, eps, mu, false);
        System.out.println("Scan completed!\n");
        clusters = graphDB.findClusters(labels);
        System.out.println(clusters);

        calculateMeasures(graphDB.clustersDB, clusters, metapath, k, eps, mu);
    }

    public static Double calculateEpsilon(Double[][] simMatrix, int personCount){
        Double eps = 0.0;
        Integer totEdges = 0;
        System.out.println(personCount);
        for(int i = 0; i < personCount; i++){
            for(int j = 0; j < personCount; j++){
                if(i != j && simMatrix[i][j] != null) {
                    eps = eps + simMatrix[i][j];
                    totEdges++;
                }
            }
        }
        eps = eps/totEdges;
        System.out.println("EPSILON: " + eps);
        System.out.println("TOT EDGES: " + totEdges);

        return eps;
    }

    public static void calculateMeasures(GraphDB clustersDB, Map<Integer, List<String>> clusters, String metapath, Integer k, Double eps, Integer mu) throws IOException {
        System.out.println();
        System.out.println("Clustering Nodes: " + clustersDB.personCount);
        System.out.println("Clustering Edges: " + clustersDB.totEdges);
        System.out.println("Number of Clusters: " + clusters.size());

        Double mod = Modularity.calculateModularity(clustersDB, clusters);
        System.out.println("Modularity: " + mod);
        Double cov = Coverage.calculateCoverage(clustersDB);
        System.out.println("Coverage: " + cov);
        Double perf = Performance.calculateClusteringPerformance(clustersDB, clusters);
        System.out.println("Performance: " + perf);
        Double cond = Conductance.calculateConductance(clustersDB, clusters);
        System.out.println("Conductance: " + cond);

        saveResult(metapath, clustersDB, clusters, k, eps, mu, mod, cov, perf, cond, null);
    }

    public static void saveResult(String metapath, GraphDB clustersDB, Map<Integer, List<String>> clusters, Integer k, Double eps, Integer mu, Double mod, Double cov, Double perf, Double cond, Double sil) throws IOException {
        File file = new File("src/main/java/org/openjfx/dmProject/Measures/Results/"+ metapath +".txt");

        FileWriter txtWriter = new FileWriter(file,true);
        txtWriter.append(metapath).append(": k = ").append(String.valueOf(k)).append(" eps = ").append(String.valueOf(eps)).append(", mu = ").append(String.valueOf(mu)).append("\n");
        txtWriter.append("Clustering Nodes: ").append(String.valueOf(clustersDB.personCount)).append("\n");
        txtWriter.append("Clustering Edges: ").append(String.valueOf(clustersDB.totEdges)).append("\n");
        txtWriter.append("Number of Clusters: ").append(String.valueOf(clusters.size())).append("\n");
        txtWriter.append("Modularity: ").append(String.valueOf(mod)).append("\n");
        txtWriter.append("Coverage: ").append(String.valueOf(cov)).append("\n");
        txtWriter.append("Performance: ").append(String.valueOf(perf)).append("\n");
        txtWriter.append("Conductance: ").append(String.valueOf(cond)).append("\n");
        txtWriter.append("Clusters:\n").append(String.valueOf(clusters)).append("\n");
        txtWriter.append("\n");

        txtWriter.flush();
        txtWriter.close();
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
