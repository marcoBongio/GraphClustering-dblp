.PHONY: all clean

all: pom.xml src/main/java/org/openjfx/dmProject/Main.java
	mvn clean install
	make run
run:
	mvn exec:java