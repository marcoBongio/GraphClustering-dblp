package org.openjfx.dmProject.Clustering;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.openjfx.dmProject.Graph.GraphDB;
import org.openjfx.dmProject.Graph.NodeType;
import org.openjfx.dmProject.Graph.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scan {

    public static List<String> findNeighborhood(Double[][] matrix, String id) {
        //Returns the neighbors(N), as well as all the connected vertices(vcols)
        int index = Integer.parseInt(id);
        List<String> N = new ArrayList<>();

        for(int i = 0; i < matrix[0].length; i++){
            if(matrix[index][i] != null && i != index) {
                N.add(Integer.toString(i));
            }
        }
        N.add(Integer.toString(index));

        return N;
    }

    public static List<String> findNeighborhood(Double[][] matrix, String id, Double eps) {
        //Returns the neighbors(N), as well as all the connected vertices(vcols)
        int index = Integer.parseInt(id);
        List<String> N = new ArrayList<>();
        Double sigma;
        List<String> indexN, iN = new ArrayList<>();
        int countInt;

        for(int i = 0; i < matrix[0].length; i++){
            if(matrix[index][i] != null && i != index) {
                indexN = findNeighborhood(matrix, Integer.toString(index));
                iN = findNeighborhood(matrix, Integer.toString(i));

                countInt = sizeInt(indexN,iN);
                sigma = (1 + matrix[index][i]) * (countInt/(Math.sqrt(indexN.size() * iN.size())));

                if (sigma >= eps)
                    N.add(Integer.toString(i));
            }
        }

        return N;
    }

    private static int sizeInt(List<String> indexN, List<String> iN) {
        int size = 0;
        for(String i:indexN){
            if(iN.contains(i))
                size++;
        }

        return size;
    }

    public static int[] clusterScan(GraphDB graphDB, Double[][] matrix, String metapath, int k, Double eps, Integer mu, boolean app) {
        int[] vertex_labels = new int[graphDB.personCount];
        //All vertices are labeled as unclassified(-1)
        Arrays.fill(vertex_labels, -1);
        List<String> unlab_list = Utils.findAllPerson(graphDB);
        //start with a neg core(every new core we incr by 1)
        int cluster_id = -1;

        for (String u : unlab_list) {
            if (vertex_labels[Integer.parseInt(u)] == -1) {
                List<String> N = findNeighborhood(matrix, u, eps);
                //include itself
                N.add(0, u);

                //if vertex is core
                if (N.size() >= mu) {
                    //print "we have a cluster at: %d ,with length %d " % (vertex, len(N) - 1)
                    //System.out.println("Cluster at:" + vertex + "with length" + N.size());
                    //gen a new cluster id (0 indexed)
                    cluster_id += 1;
                    List<String> Q;
                    Q = N;

                    while (!Q.isEmpty()) {
                        String w = Q.get(0);
                        List<String> R = findNeighborhood(matrix, w, eps);
                        //include itself
                        R.add(0, w);
                        //(struct reachable) check core and if y is connected to vertex
                        if (R.size() >= mu) {
                            for (String s : R) {
                                //print "we have a structure Reachable at: %d ,with length %d " % (y, len(R) - 1)
                                //System.out.println("\t"+"\t"+"Reachable at:" + y + "with length" + R.size());
                                int label = vertex_labels[Integer.parseInt(s)];
                                //if unclassified or non-member
                                if (label == -1 || label == -4) {
                                    vertex_labels[Integer.parseInt(s)] = cluster_id;
                                    //System.out.println(s + ":" + cluster_id);
                                }
                                //if unclassified
                                if (label == -1) {
                                    Q.add(s);
                                }
                            }
                        }
                        Q.remove(w);
                    }
                } else {
                    vertex_labels[Integer.parseInt(u)] = -4;
                }
            }
        }

        //classify non-members
        for(int i = 0; i < vertex_labels.length; i++){
            //number of node connected to i
            List<String> ncols = new ArrayList<>();
            if(vertex_labels[i] == -4) {
                for(int j = 0; j < vertex_labels.length; j++) {
                    //if i and j are connected
                    if (matrix[i][j] != null) {
                        String label_j = Integer.toString(vertex_labels[j]);
                        //if j is inside a cluster and the cluster it's not in ncols
                        if (vertex_labels[j] > 0 && !ncols.contains(label_j))
                            ncols.add(label_j);
                    }
                }
                //if at two node linked to i belong to two different clusters
                if(ncols.size() >= 2) {
                    //mark as a hub
                    vertex_labels[i] = -2;
                } else {
                    //mark as outlier
                    vertex_labels[i] = -3;
                }
            }
        }

        try {
            if(!app) {
                graphDB.clustersDB = null;
                graphDB.clustersDB = GraphDB.createClusterDB(vertex_labels, matrix, metapath, k, mu);
            } else {
                for (int c = 0; c < vertex_labels.length; c++) {

                    try (Transaction tx = graphDB.beginTx()) {
                        if(vertex_labels[c] == -1)
                            System.out.println(c + ":" + vertex_labels[c]);
                        Node p = tx.findNode(NodeType.Person, "id", (Integer.toString(c)));
                        try {
                            p.setProperty("cluster", vertex_labels[c]);
                        } catch (Error ignored) {}

                        tx.commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Error ignored) { }

        return vertex_labels;
    }

}
