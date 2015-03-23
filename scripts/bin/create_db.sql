drop database if exists performance_overview;
create database performance_overview;
use performance_overview;

drop table if exists `values`;
create table `values`(
	id int auto_increment not null primary key,
	frame varchar(255),
	nr_split int,	
	total_turns int,

    input varchar(1024),

	turns int,

	tasks int,
	property varchar(255),

	value int, 

    is_map char(1)
) ENGINE=INNODB;
