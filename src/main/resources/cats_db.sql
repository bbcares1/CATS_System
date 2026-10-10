USE cats_db;

INSERT INTO users (user_id,user_name,password,name,designation,role) 
VALUES
(1,'ryan','admin123','Ryan','Administrator','ADMIN'),
(2,'michael','pass123','Michael','Team Leader','MANAGER'),
(3,'junie','pass123','Junie','Professional','STAFF'),
(4,'fan','pass123','Fan','Professional','STAFF'),
(5,'martin','pass123','Martin','Professional','STAFF'),
(6,'imran','pass123','Imran','Professional','STAFF'),
(7,'owen','pass123','OWen','Professional','STAFF'),
(8,'jialu','pass123','Jialu','Professional','STAFF');

INSERT INTO admin (user_id,staff_no) 
VALUES (1,'A001');

INSERT INTO staff (user_id,staff_id,training_budget,training_days) 
VALUES
(2,'M0001',5000.00,15),
(3,'S0003',3000.00,10),
(4,'S0004',3000.00,10),
(5,'S0005',3000.00,10),
(6,'S0006',3000.00,10),
(7,'S0007',3000.00,10),
(8,'S0008',3000.00,10);
INSERT INTO manager (user_id) 
VALUES (2);

