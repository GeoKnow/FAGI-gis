INSERT INTO fused_geometries (subject_A, subject_B, geom)
SELECT links.nodea, 
	links.nodeb, 
	ST_scale(dataset_a_geometries.geom,?,?)
FROM links INNER JOIN dataset_a_geometries 
ON (links.nodea = dataset_a_geometries.subject)
