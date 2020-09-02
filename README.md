# Graph Clustering DBLP #

The project is composed by:
- a python script used to gather informations from the DBLP website and save them in a different .csv file
- a java application that create a Neo4j database from the previous .csv file and apply a weighted version of scan clustering algorithm on the database

In addition to the clustering task, the java application compute some graph clustering metrics to estimate the clustering quality:
- Conductance
- Coverage
- Modularity
- Performance

<p align="center">
  <img src="https://github.com/marcoBongio/GraphClustering-dblp/blob/master/ScanResult.png">
</p>


