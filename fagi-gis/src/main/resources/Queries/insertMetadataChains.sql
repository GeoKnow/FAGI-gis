SELECT ?s ?p ?o1 ?p4 ?o3 ?o ?p1 ?o2
FROM <http://localhost:8890/DAV/links>
FROM <http://localhost:8890/DAV/uni>
FROM <http://localhost:8890/DAV/wiki>
WHERE {
	?s <http://www.w3.org/2002/07/owl#sameAs> ?o
	{ ?s ?p ?o1 . OPTIONAL { ?o1 ?p4 ?o3. } } UNION
	{ ?o ?p1 ?o2 .}

	FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\"))
	FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))
	FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\"))
	FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))
	FILTER(!regex(?p, \"http://www.w3.org/2002/07/owl#sameAs\", \"i\"))
	FILTER(!regex(?p1,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\"))
	FILTER(!regex(?p1, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))
	FILTER(!regex(?p1, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\"))
	FILTER(!regex(?p1, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\")) \n}
}
        
