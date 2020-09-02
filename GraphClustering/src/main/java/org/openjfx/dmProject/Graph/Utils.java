package org.openjfx.dmProject.Graph;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {
	public static void registerShutdownHook( DatabaseManagementService managementService )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                managementService.shutdown();
            }
        } );
    }

    public static List<String[]> parseFile(String path, String reg) throws IOException {
        List<String[]> ret = new ArrayList<>();
        File file = new File(path);

        if (!file.exists()) {
            return ret;
        }

        BufferedReader br = new BufferedReader(new FileReader(file));
        String contentLine = br.readLine();

        while (contentLine != null) {
            String[] splited = contentLine.trim().split(reg);
            ret.add(splited);
            contentLine = br.readLine();
        }
        br.close();

        return ret;
    }


    public static List<Map.Entry<String, Double>> sortMap(Map<String, Double> map) {
        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return list;
    }


    public static List<String> findAllPerson(GraphDB graphDB){
        List<String> all = new ArrayList<>();
        String query = "MATCH (p:Person) return p.id";
        try (Transaction ignored = graphDB.beginTx();
             Result result = ignored.execute(query)) {
            graphDB.personCount = 0;
            while (result.hasNext()) {
                Map<String, Object> row = result.next();
                String value = (String) row.get("p.id");
                if (value != null) {
                    all.add(value);
                    graphDB.personCount++;
                }
            }
        }
        return all;
    }

}
