--Database creation script for the importer PostGIS schema

--Drop all tables if they exist
DROP TABLE IF EXISTS dataset_a_info;
DROP TABLE IF EXISTS dataset_a_metadata;
DROP TABLE IF EXISTS dataset_a_geometries;
DROP TABLE IF EXISTS dataset_b_info;
DROP TABLE IF EXISTS dataset_b_metadata;
DROP TABLE IF EXISTS dataset_b_geometries;
DROP TABLE IF EXISTS fused_geometries;


--Create a table to hold datasetA's info
CREATE TABLE dataset_a_info (
	endpoint text NOT NULL,
	graph text NOT NULL
);

--Create a table to hold datasetA's metadata
CREATE TABLE dataset_a_metadata (
	id serial PRIMARY KEY,
	subject text NOT NULL,
	predicate text NOT NULL,
	object text NOT NULL,
	object_lang text,
	object_datatype text
);

CREATE INDEX idx_dataset_a_metadata_subject ON dataset_a_metadata USING btree (subject);

--Create a table to hold datasetA's geometries
CREATE TABLE dataset_a_geometries (
	id serial PRIMARY KEY,
	subject text NOT NULL
);

SELECT AddGeometryColumn('dataset_a_geometries', 'geom', 4326, 'GEOMETRY', 2);
CREATE INDEX idx_dataset_a_geometries_geom ON dataset_a_geometries USING gist (geom);
CLUSTER dataset_a_geometries USING idx_dataset_a_geometries_geom;

--Create a table to hold datasetB's info
CREATE TABLE dataset_b_info (
	endpoint text NOT NULL,
	graph text NOT NULL
);

--Create a table to hold datasetB's metadata
CREATE TABLE dataset_b_metadata (
	id serial PRIMARY KEY,
	subject text NOT NULL,
	predicate text NOT NULL,
	object text NOT NULL,
	object_lang text,
	object_datatype text
);

CREATE INDEX idx_dataset_b_metadata_subject ON dataset_b_metadata USING btree (subject);

--Create a table to hold datasetB's geometries
CREATE TABLE dataset_b_geometries (
	id serial PRIMARY KEY,
	subject text NOT NULL
);

SELECT AddGeometryColumn('dataset_b_geometries', 'geom', 4326, 'GEOMETRY', 2);
CREATE INDEX idx_dataset_b_geometries_geom ON dataset_b_geometries USING gist (geom);
CLUSTER dataset_b_geometries USING idx_dataset_b_geometries_geom;

--Create a table to hold fused geometries
CREATE TABLE fused_geometries (
	id serial PRIMARY KEY,
	subject_A text NOT NULL,
	subject_B text NOT NULL
);

SELECT AddGeometryColumn('fused_geometries', 'geom', 4326, 'GEOMETRY', 2);
CREATE INDEX idx_fused_geometries_geom ON fused_geometries USING gist (geom);

