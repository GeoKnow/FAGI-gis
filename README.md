#fagi-gis-service
___
##Dependencies
  * FAGI-gis is compatible with _virtuoso-opensource-7.1_ and _7.2_, _Postgresql-9.3_ and tested in _Linux x64_ _Mac OS X 10.6+_ and _Windows 7_. 
  * FAGI-gis uses _wordnet_ for schema matching regarding the metadata fusion, and needs to be installed on your system: 
    1. _Linux_ `sudo apt-get install wordnet`.
    2. _Mac OS X_ requires a custom build of Wordnet which requires an existing X11 installation.
    3. _Windows_ support an older version of Wordnet which can be downloaded from [here](https://wordnet.princeton.edu/wordnet/download/) 
  * FAGI-gis requires an existing installation of Apache Tomcat 7 for the web interface.

##Building Instructions

Both projects are maintained using MAVEN. fagi-gis-service depends on fagi-gis lib

* **Command line:**
  1. Download or checkout the FAGI-gis repo at a path of your choice:  
  `git clone https://github.com/GeoKnow/FAGI-gis targetDir`
  2. Install in local maven repo the following two jars (not provided by maven repository).  
    * Download:
  [Virtuoso Jena 2.10.x Provider JAR file ](http://opldownload.s3.amazonaws.com/uda/virtuoso/rdfproviders/jena/210/virt_jena2.jar)
  and
  [Virtuoso JDBC 4 Driver JAR file](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload/virtjdbc4.jar). 
  They are also provided [here] (https://github.com/GeoKnow/FAGI-gis/tree/develop/fagi-gis/lib)
 
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
  
##How to Run

#FAGI-gis CLI version

* **Command line:**
  1. Open the *fusion.conf* file and provide the appropriate configuration, 
  2. Go to fagi-gis/target from terminal and run:  
`java -jar fagi-gis -c /path/to/fusion.conf`
* **Netbeans:**
  1. After you have built the Project, provide the appropriate information in the *fusion.conf* file. 
  2. Right click on the Project and click *Properties*. 
  3. Go to *"Run"* tab and at the *Arguments* field add "-c /path/to/fusion.conf" and click OK.
  4. Click *"Run"*.
  
#FAGI-gis Web Interface version

* **Command line:**  

  1. Add the produced *fagi-gis-service.war* in your _Apache Tomcat 7_ webapps directory.
  2. Open your browser at *"http://localhost:port/fagi-gis-service"* and use the FAGI-gis Interface!

* **Netbeans:**
  1. Add _Apache Tomcat 7_ to the project libraries. 
  2. Click *"Clean and build Project"*.
  3. Click *"Run"* and the default browser of Netbeans will open automatically with FAGI-gis Interface!
  
#Licence
___
The source code of this repo is published under the Apache License Version 2.0