package org.openjfx.dmProject.Graph;

import lombok.Data;
import org.neo4j.graphdb.*;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.openjfx.dmProject.Clustering.ClusterRelTypes;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Data
public class GraphDB {
    private DatabaseManagementService managementService;
    public int personCount = 0;
    public double totEdges = 0;
    public GraphDatabaseService graphDB;
    public GraphDB clustersDB;

    public GraphDB(String graphDBPath) {
        managementService = new DatabaseManagementServiceBuilder( new File(graphDBPath)).build();
        graphDB= managementService.database( DEFAULT_DATABASE_NAME );
        Utils.registerShutdownHook( managementService );
    }

    public void initializeGraphDB(String dataDir) {
        try (Transaction tx = this.graphDB.beginTx()) {
                tx.schema()
                        .constraintFor(NodeType.Person)
                        .assertPropertyIsUnique( "id" )
                        .withName( "idPers" )
                        .create();
                tx.schema()
		                .constraintFor(NodeType.Paper)
		                .assertPropertyIsUnique( "id" )
		                .withName( "idPaper" )
		                .create();
                tx.schema()
		                .constraintFor(NodeType.Venue)
		                .assertPropertyIsUnique( "id" )
		                .withName( "idVenue" )
		                .create();
                tx.commit();
                buildNodeRelation(dataDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Transaction beginTx() {
        return graphDB.beginTx();
    }

    private void buildNodeRelation(String dataDir) throws IOException {
        List<String[]> authors = Utils.parseFile(dataDir + "persons.csv", "\\|");
        int id = 0;
        try ( Transaction tx = graphDB.beginTx() ){
            System.out.println("Creating Authors Nodes...");
        	for (String[] item : authors) {
	            Node node = tx.createNode(NodeType.Person);
	            node.setProperty("id", Integer.toString(id));
	            node.setProperty("name", item[0]);
	            node.setProperty("URL", item[1]);
	            try {
	                node.setProperty("affiliation", item[2]);
	            } catch(Exception ignored) {}
	            id++;
        	}
        	tx.commit();
        }

        List<String[]> venues = Utils.parseFile(dataDir + "venues.csv", "\\|");
        id = 0;
        try ( Transaction tx = graphDB.beginTx() )
        {
            System.out.println("Creating Venues Nodes...");
	        for (String[] item : venues) {
	            Node node = tx.createNode(NodeType.Venue);
	            node.setProperty("id", Integer.toString(id));
	            node.setProperty("name", item[0]);
	            node.setProperty("URL", item[1]);
	            id++;
	        }
	        tx.commit();
        }

        List<String[]> papers = Utils.parseFile(dataDir + "papers.csv", "\\|");
        id = 0;
        try ( Transaction tx = graphDB.beginTx() )
        {
            System.out.println("Creating Papers Nodes...");
	        for (String[] item : papers) {
	            Node node = tx.createNode(NodeType.Paper);
	            node.setProperty("id", Integer.toString(id));
	            node.setProperty("title", item[0]);
	            node.setProperty("URL", item[1]);
	            node.setProperty("type", item[2]);
	            node.setProperty("year", item[3]);
	            id++;
	        }
        tx.commit();
		}

        List<String[]> coauthor = Utils.parseFile(dataDir + "authors.csv", "\\|");
        try ( Transaction tx = graphDB.beginTx() )
        {
            System.out.println("Creating AP Relationships...");
            for (String[] item : coauthor) {
                String idPaper = item[0];
                String idPerson = item[1];

                Node paper = tx.findNode(NodeType.Paper, "URL", idPaper);

                Node person = tx.findNode(NodeType.Person, "URL", idPerson);
                person.getLabels();
                person.createRelationshipTo(paper, RelTypes.COAUTHOR_OF);
            }
            tx.commit();
        }

        List<String[]> presented = Utils.parseFile(dataDir + "publications.csv", "\\|");
        try ( Transaction tx = graphDB.beginTx() )
        {
            System.out.println("Creating PV Relationships...");
            for (String[] item : presented) {
                String idPaper = item[0];
                String idVenue = item[1];

                Node paper = tx.findNode(NodeType.Paper, "URL", idPaper);

                Node venue = tx.findNode(NodeType.Venue, "URL", idVenue);
                if (venue != null) {
                    paper.createRelationshipTo(venue, RelTypes.PRESENTED_BY);
                }
            }
            tx.commit();
        }

        System.out.println("Database created successfully\n");
    }

    private Map<String, Integer> getReletedAuthor(String metapath, String id) {
        String query = "MATCH (:Person {id:'"+id+"'})-[:COAUTHOR_OF]->(:Paper)<-[:COAUTHOR_OF]-(d:Person) WITH d MATCH (d)-[:COAUTHOR_OF]->(:Paper)<-[:COAUTHOR_OF]-(p:Person) RETURN p.id";
        if(metapath.equals("APA")){
            query = "MATCH (:Person {id:'"+id+"'})-[:COAUTHOR_OF]->(x:Paper) WITH x MATCH(x)<-[:COAUTHOR_OF]-(p:Person) RETURN p.id";
        }
        if(metapath.equals("APVPA")){
            query = "MATCH (:Person {id:'"+id+"'})-[:COAUTHOR_OF]->(:Paper)-[:PRESENTED_BY]->(v:Venue) WITH v MATCH (v)<-[:PRESENTED_BY]-(:Paper)<-[:COAUTHOR_OF]-(p:Person) RETURN p.id";
        }
        Map<String, Integer> paths = new HashMap<>();
        try (Transaction ignored = this.graphDB.beginTx();
             Result result = ignored.execute(query)) {
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String value = (String) row.get("p.id");
                if (paths.containsKey(value)) {
                    paths.put(value, paths.get(value) + 1);
                } else {
                    paths.put(value, 1);
                }
            }
        }

        return paths;
    }

    private int getPathNum(String metapath, String startid, String endid) {
        String query = "MATCH (:Person {id:'"+startid+"'})-[:COAUTHOR_OF]->(:Paper)<-[:COAUTHOR_OF]-(d:Person) WITH d MATCH (d)-[:COAUTHOR_OF]->(:Paper)<-[:COAUTHOR_OF]-(p:Person{id:'"+endid+"'}) RETURN p.id";
        if(metapath.equals("APA")){
            query = "MATCH (:Person {id:'"+startid+"'})-[:COAUTHOR_OF]->(x:Paper) WITH x MATCH(x)<-[:COAUTHOR_OF]-(p:Person{id:'"+endid+"'}) RETURN p.id";
        }
        if(metapath.equals("APVPA")){
            query = "MATCH (:Person {id:'"+startid+"'})-[:COAUTHOR_OF]->(:Paper)-[:PRESENTED_BY]->(v:Venue) WITH v MATCH (v)<-[:PRESENTED_BY]-(:Paper)<-[:COAUTHOR_OF]-(p:Person{id:'"+endid+"'}) RETURN p.id";
        }
        int count = 0;
        try (Transaction ignored = this.graphDB.beginTx();
             Result result = ignored.execute(query)) {
            while (result.hasNext()) {
                result.next();
                count++;
            }
        }
        return count;
    }


    /**
     * Find the TopK similar Author and print the score
     *  @param metapath metapath to use
     * @param id       author Id
     * @param k        TopK similar authors
     * @return list
     */
    public List<Map.Entry<String, Double>> getTopKSimAuthors(String metapath, String id, int k) {
        Map<String, Double> sims = new HashMap<>();

        Map<String, Integer> related = getReletedAuthor(metapath, id);
        if (related.size() == 0) {
            sims.put(id, 1.0);
            return Utils.sortMap(sims);
        }

        int score2 = related.get(id);   // x->x

        for (Map.Entry<String, Integer> r : related.entrySet()) {
            int score1 = r.getValue(); // x->y
            int score3 = getPathNum(metapath, r.getKey(), r.getKey());      // y->y

            double sim = 2.0 * score1 / (score2 + score3);

            sims.put(r.getKey(), sim);
        }

        List<Map.Entry<String, Double>> list = Utils.sortMap(sims);
        List<Map.Entry<String, Double>> listK = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i == k) {
                break;
            }

            Map.Entry<String, Double> item = list.get(i);
            listK.add(list.get(i));
            try (Transaction ignored = this.graphDB.beginTx()) {
                Node person = ignored.findNode(NodeType.Person, "id", item.getKey());
                //String name = String.valueOf(person.getProperty("name"));
                //System.out.println(name + ":" + item.getValue());
            }
        }
        return listK;
    }

    public Double[][] createMatrix(String metapath, List<String> list, int k) throws IOException {
        System.out.println("Creating Similarity " + metapath + k +" Matrix...");
        Double[][] matrix = new Double[personCount][personCount];
        List<Map.Entry<String, Double>> l;
        String path = "src/main/java/org/openjfx/dmProject/"+ metapath + k +".txt";
        File file = new File(path);
        int i = 0;

        if(file.exists()){
            List<String[]> mat = Utils.parseFile(path, " ");
            for(String[] row:mat){
                for(int j = 0; j < personCount; j++){
                    if(Double.parseDouble(row[j]) != 0) {
                        matrix[i][j] = Double.parseDouble(row[j]);
                        matrix[j][i] = Double.parseDouble(row[j]);
                    }
                }
                i++;
            }
            return matrix;
        }

        FileWriter txtWriter = new FileWriter(file);

        for (String rowMain : list) {
            int indexMain = Integer.parseInt(rowMain);
            l = getTopKSimAuthors(metapath, rowMain, k);
            for (Map.Entry<String, Double> row : l) {
                int index = Integer.parseInt(row.getKey());

                matrix[indexMain][index] = row.getValue();
                matrix[index][indexMain] = row.getValue();
            }

        }

        for (i = 0; i < personCount; i++) {
            for (int j = 0; j < personCount; j++){
                if (matrix[i][j]==null)
                {txtWriter.append("0");}
                else {
                txtWriter.append(Double.toString(matrix[i][j]));
                }
                txtWriter.append(" ");}
                
            txtWriter.append("\n");
        }
        txtWriter.flush();
        txtWriter.close();
        return matrix;
    }

    public Map<Integer, List<String>> findClusters(int[] vertex_labels) {
        List<Integer> checked = new ArrayList<>();
        TreeMap<Integer, List<String>> clusters = new TreeMap<>();
        System.out.println("CLUSTERS:");
        for (int cluster_id : vertex_labels) {
            List<String> nodes = new ArrayList<>();
            if (!checked.contains(cluster_id) && cluster_id >= 0) {
                for (int j = 0; j < vertex_labels.length; j++) {
                    if (vertex_labels[j] == cluster_id)
                        nodes.add(Integer.toString(j));
                }

                clusters.put(cluster_id, nodes);
                checked.add(cluster_id);
            }
        }

        return clusters;
    }

    public static GraphDB createClusterDB(int[] vertex_labels, Double[][] sim, String metapath, int k, Integer mu) {
        System.out.println("Creating Cluster DB...");
        String dataPath = "target/clusters/SCAN/" + metapath + "/" + metapath + "k" + k + "mu" + mu;
        boolean check_dir = new File(dataPath).exists();
        GraphDB clusterDB = new GraphDB(dataPath);

        try (Transaction tx = clusterDB.beginTx()) {

            if(check_dir){
                System.out.println("Cluster DB Created!\n");
                String query = "MATCH (p:Person) RETURN count(p) AS count";
                Result result = tx.execute(query);
                Map<String, Object> row = result.next();
                clusterDB.personCount = Math.toIntExact((Long) row.get("count"));
                System.out.println("NODES: " + clusterDB.personCount);

                String query2 = "MATCH (:Person)-[c:COAUTHOR]->(:Person) RETURN count(c) AS count";
                Result result2 = tx.execute(query2);
                Map<String, Object> row2 = result2.next();
                clusterDB.totEdges =(Long) row2.get("count");
                System.out.println("EDGES: " + clusterDB.totEdges);

                return clusterDB;
            }

            for (int c = 0; c < vertex_labels.length; c++) {
                Node p = tx.createNode(NodeType.Person);
                p.setProperty("id", Integer.toString(c));
                p.setProperty("cluster", vertex_labels[c]);
            }

            for (int i = 0; i < vertex_labels.length; i++) {
                String index = Integer.toString(i);
                Node person = tx.findNode(NodeType.Person, "id", index);
                for (int j = 0; j < vertex_labels.length; j++) {
                    if (sim[i][j] != null && i != j) {
                        Node person2 = tx.findNode(NodeType.Person, "id", Integer.toString(j));
                        Relationship similarity = person.createRelationshipTo(person2, ClusterRelTypes.COAUTHOR);
                        similarity.setProperty("similarity", sim[i][j]);
                    }
                }

            }

            System.out.println("Cluster DB Created!");
            String query = "MATCH (p:Person) RETURN count(p) AS count";
            Result result = tx.execute(query);
            Map<String, Object> row = result.next();
            clusterDB.personCount = Math.toIntExact((Long) row.get("count"));
            System.out.println("NODES: " + clusterDB.personCount);

            String query2 = "MATCH (:Person)-[c:COAUTHOR]->(:Person) RETURN count(c) AS count";
            Result result2 = tx.execute(query2);
            Map<String, Object> row2 = result2.next();
            clusterDB.totEdges =(Long) row2.get("count");
            System.out.println("EDGES: " + clusterDB.totEdges);


            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return clusterDB;
    }
}
