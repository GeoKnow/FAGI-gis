INSERT INTO fused_geometries (subject_A, subject_B, geom) 
SELECT links.nodea, links.nodeb, a.geom 
FROM links INNER JOIN dataset_a_geometries AS a
ON (links.nodea = a.subject)

INSERT INTO fused_geometries (subject_A, subject_B, geom) 
SELECT links.nodea, links.nodeb, b.geom 
FROM links INNER JOIN dataset_b_geometries AS b
ON (links.nodeb = b.subject)
