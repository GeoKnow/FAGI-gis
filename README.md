#FAGI-gis-CLI 
___
##Building Instructions
* **Command line:**
  1. Download or checkout the FAGI-gis repo at a path of your choice:  
  `git clone https://github.com/GeoKnow/FAGI-gis targetDir`
  2. Install in local maven repo the following two jars (not provided by maven repository).  
    * Download:
  [Virtuoso Jena 2.10.x Provider JAR file ](http://opldownload.s3.amazonaws.com/uda/virtuoso/rdfproviders/jena/210/virt_jena2.jar)
  and
  [Virtuoso JDBC 4 Driver JAR file](http://virtuoso.openlinksw.com/dataspace/doc/dav/wiki/Main/VOSDownload/virtjdbc4.jar).  
    * Run:  
`mvn install:install-file -Dfile=/path/to/virt_jena2.jar -DgroupId=virtuoso.jena.driver -DartifactId=virtjena -Dversion=2 -Dpackaging=jar`
      * Run:  
`mvn install:install-file -Dfile=/path/to/virtjdbc4.jar -DgroupId=virtuoso -DartifactId=vjdbc41 -Dversion=4.1 -Dpackaging=jar`  
  3. Go to /path/to/targetDir/FAGI-gis-CLI and build the project by running:  
`mvn package`

* **Netbeans:**
  1. Download or checkout the FAGI-gis repo at a path of your choice as above:  
`git clone https://github.com/GeoKnow/FAGI-gis targetDir`  
  2. In Netbeans, File -> Open Project.  
Browse and choose FAGI-gis-CLI and click OK. 
  3. Install the missing jars locally:  
    * Download the two jars from the links provided above.
    * Click on the dependencies of the project, find the two missing jars, right click on them and then click *"Manually install artifact"*.
    * Browse to the location of the jars and click OK.  
  4. Click *"Build Project"*.
  
##How to Run

* **Command line:**
  1. Open the *fusion.conf* file and provide the appropriate configuration, 
  2. Go to FAGI-gis-CLI/target from terminal and run:  
`java -jar FAGI-gis-CLI -c /path/to/fusion.conf`
* **Netbeans:**
  1. After you have built the Project, provide the appropriate information in the *fusion.conf* file. 
  2. Right click on the Project and click *Properties*. 
  3. Go to *"Run"* tab and at the *Arguments* field add "-c /path/to/fusion.conf" and click OK.
  4. Click *"Run"*.
  
#FAGI-gis-WebInterface
___
##Building and Running Instructions

* **Command line:**  

  1. After you have checked out or downloaded the project, go to FAGI-gis-WebInterface directory and run:
`ant -f build.xml`
  2. Add the produced *FAGI-WebInterface.war* in your _Apache Tomcat 7_ webapps directory.
  3. Open your browser at *"http://localhost:port/FAGI-WebInterface"* and use the FAGI-gis Interface!

* **Netbeans:**

  1. In Netbeans, File -> Open Project. Browse and choose FAGI-gis-WebInterface and click OK (if you have installed java web plugin in Netbeans, FAGI-gis-WebInterface should have the appropriate web project icon).
  2. Add _Apache Tomcat 7_ to the project libraries. 
  3. Click *"Clean and build Project"*.
  4. Click *"Run"* and the default browser of Netbeans will open automatically with FAGI-gis Interface!
  
##Notes
  * FAGI-gis is compatible with _virtuoso-opensource-7.0_ and _7.1_, _Postgresql-9.3_ and tested in _Linux x64_. 
  * FAGI-gis uses _wordnet_ for schema matching regarding the metadata fusion. If you don' t have it installed on your system type: 
`sudo apt-get install wordnet`.
  * FAGI-gis-WebInterface uses FAGI-gis-CLI as a library, but you can build and run the projects independently because we ship the _FAGI-gis-CLI.jar_ inside the FAGI-gis-WebInterface dependencies.

#Licence
___
The source code of this repo is published under the Apache License Version 2.0
