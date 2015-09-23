#Development Version
![](https://raw.githubusercontent.com/GeoKnow/FAGI-gis/develop/doc/fagi_logo.jpg)

FAGI-gis is a tool developed, mainly, to facilitate the fusion of interlinked RDF entities containing spatial data. It is designed to retrieve data through SPARQL endpoints. This allows for FAGI-gis to operate on already existing and publicly available datasets without the need for any special formatting or input. It also supports the fusion and handling of other, non-spatial metadata related to these entities. 

The user provides the tool with two source datasets and a list of linked entities between them, either in file format or through an available SPARQL endpoint. The tool analyzes the datasets, discovering how geometric information is stored along with their accompanied metadata. Knowing the data structure, FAGI-gis offers the user various options and recommendations for fusing each entity pair into a new, fused, richer entity.

It consists of two components, namely, fagi-gis-cli and fagi-gis-service. The first offers a command line interface for basic fusion functionality and the latter provides a full fledged interactive user interface for advanced previewing and fusion actions on geometric data and their metadata.

On Linux, FAGI-gis comes as part of the GeoKnow Generator and the latest stable version can be installed through the [ldstack repository](http://stack.linkeddata.org/getting-started/geoknow-generator/). After, setting up the generator' s repository, a simple 

`sudo apt-get install fagi-gis-common fagi-gis fagi-gis-service`

will install the tool on your local machine.  

On Windows, there is an installer that comes with all the required components. The installer offers to create three shortcuts on the Desktop and it is **strongly** recommended to accept them for ease of use.
 1. A shortcut for starting tomcat7 (Currently requires to be **Run As Administrator**)
 2. A shortcut to start the provide Virtuoso triple store (Currently requires to be **Run As Administrator**)
 3. A shortcut to the FAGI-gis web interface

The main interface consists of a menu bar and a map preview. The user supplies his data sources and is presented with several options to control the fusion process. Geometric information is previewed on the map at all stages.


![](https://raw.githubusercontent.com/GeoKnow/FAGI-gis/develop/doc/fusion_demo.png)

##Manually Building FAGI-gis

Following is the set of dependency requirements.
Mainly, FAGI-gis requires a Virtuoso Quad Store to handle the information in RDF format
and a Postgres/Postgis installation for the geospatial data.

___
##Dependencies
  * FAGI-gis is compatible with _virtuoso-opensource-7.1_ and _7.2_, _Postgresql-9.3_ and tested in _Linux x64_ _Mac OS X 10.6+_ and _Windows 7_. 
  * The latest Virtuoso is provided in binary form for Windows and needs to be compiled from source on Linux and Moac OS X. Both can be found on the Virtuoso-opensource [github page](https://github.com/openlink/virtuoso-opensource)
  * FAGI-gis uses _wordnet_ for schema matching regarding the metadata fusion, and needs to be installed on your system: 
    1. _Linux_ `sudo apt-get install wordnet`.
    2. _Mac OS X_ requires a custom build of Wordnet which requires an existing X11 installation.
    3. _Windows_ support an older version of Wordnet which can be downloaded from [here](https://wordnet.princeton.edu/wordnet/download/) 
  * FAGI-gis requires an existing installation of Apache Tomcat 7 for the web interface.
  * Install PostgreSQL with postgis extension:
    1. _Linux_ `sudo apt-get install postgresql-9.3-postgis-2.1 -f`.
    2. _Windows_: download PostgreSQL from [here](http://www.enterprisedb.com/products-services-training/pgdownload#windows) and install it along with the postgis extension.
   
Both projects are maintained using MAVEN. fagi-gis-service depends on fagi-gis lib

* **Command line:**
  1. Download or checkout the FAGI-gis repo at a path of your choice:  
  `git clone https://github.com/GeoKnow/FAGI-gis targetDir`
  2. Install in local maven repo the following two jars (not provided by maven repository).  
    * Download:
  Virtuoso Jena 2.10.x Provider JAR file and Virtuoso JDBC 4 Driver JAR file
  that are provided [here] (https://github.com/GeoKnow/FAGI-gis/tree/develop/fagi-gis/lib)
 
    * Run:  
`mvn install:install-file -Dfile=/path/to/virt_jena2.jar -DgroupId=virtuoso.jena.driver -DartifactId=virtjena -Dversion=2 -Dpackaging=jar`
      * Run:  
`mvn install:install-file -Dfile=/path/to/virtjdbc4.jar -DgroupId=virtuoso -DartifactId=vjdbc41 -Dversion=4.1 -Dpackaging=jar`  
  3. Go to /path/to/targetDir/fagi-gis and build the project by running:  
`mvn package`
  4. Go to /path/to/targetDir/fagi-gis-service and build the project by running:  
`mvn package`

* **Netbeans:**
  1. Download or checkout the FAGI-gis repo at a path of your choice as above:  
`git clone https://github.com/GeoKnow/FAGI-gis targetDir`  
  2. In Netbeans, File -> Open Project.  
Browse and choose fagi-gis and fagi-gis-service and click OK. 
  3. Install the missing jars locally:  
    * Download the two jars from the links provided above.
    * Click on the dependencies of the project, find the two missing jars, right click on them and then click *"Manually install artifact"*.
    * Browse to the location of the jars and click OK.  
  4. Click *"Build Project"*.

After building the tools 

#FAGI-gis Web Interface version

  1. Add the produced *fagi-gis-service.war* in your _Apache Tomcat 7_ webapps directory.
  2. Open your browser at *"http://localhost:port/fagi-gis-service"* and use the FAGI-gis Interface!

#FAGI-gis Command Line version

  1. Open the *fusion.conf* file and provide the appropriate configuration, 
  2. Go to fagi-gis/target from terminal and run:  
`java -jar fagi-gis -c /path/to/fusion.conf`

#Licence
___
The source code of this repo is published under the Apache License Version 2.0
