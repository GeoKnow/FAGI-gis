SELECT * FROM links 
INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, dataset_b_geometries.subject AS b_s,
		dataset_a_geometries.geom AS a_g, dataset_b_geometries.geom AS b_g,
		ST_X(dataset_a_geometries.geom), ST_Y(dataset_a_geometries.geom),
		 ST_X(dataset_b_geometries.geom), ST_Y(dataset_b_geometries.geom) 
		FROM dataset_a_geometries, dataset_b_geometries) AS geoms ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)
