
--Generate Tables
CREATE TABLE locations
    (id INTEGER PRIMARY KEY AUTOINCREMENT, name text, lat real, long real,
    description text, capacity int, predictionCoefficient real, predictionConstant real, predictionStartDate real);
CREATE TABLE occupants
    (id INTEGER PRIMARY KEY AUTOINCREMENT, locationId INTEGER NOT NULL, firstName text NOT NULL, lastName text NOT NULL, gender boolean NOT NULL, dependents int NOT NULL,
    phoneNumber int NOT NULL, address text NOT NULL, scanTime real NOT NULL, FOREIGN KEY(locationId) REFERENCES locations(id));

--Populate loations
INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Kent Department of Health", 41.153526, -81.352373, "No cots; pets not allowed; bottled water available", 150, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("St. Patrick Catholic Parish", 41.147254, -81.342971, "Cots available; pets allowed; food and bottled water available", 250, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Kent Department of Education", 41.156683, -81.355867, "No cots; pets not allowed; bottled water available", 25, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Masjid of Kent", 41.157580, -81.353843, "Cots available; pets allowed; food and bottled water available", 70, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Davey Elementary School", 41.157823, -81.367049, "No cots, pets allowed, food and bottled water available", 450, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Kent Free Library", 41.153352, -81.361611, "No cots; pets allowed; bottled water available", 200, 2.321, 10.955, 1568937600);

INSERT INTO locations(name, lat, long, description, capacity, predictionCoefficient, predictionConstant, predictionStartDate)
    VALUES ("Kent State University Museum", 41.152578, -81.349987, "No cots; pets allowed; food and bottled water available", 200, 2.321, 10.955, 1568937600);

--Populate People
INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (1, 'Eren', 'Jaegar', true, 149, 2, 'This is another address', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (3, 'Jacob', 'Welsh', true, 20, 114930284, 'This is an address', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (4, 'Drew', 'Polito', true, 40, 5839410, '18156 Clifton Road', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (5, 'Carter', 'Scheatzle', true, 25, 333333333, 'My name jeff', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (6, 'Robert', 'Pafford', true, 57, 898897898, 'yuh', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (7, 'Richard', 'Pignatiello', true, 2, 99, 'This is another address', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (5, 'Joe', 'Mama', true, 2, 999134, 'Yo mama’s house', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (7, 'Richard’s', 'Ankle', false, 2, 9918374, 'This is another address', 1569673923);

INSERT INTO occupants (locationId, firstName, lastName, gender, dependents, phoneNumber, address, scanTime)
    VALUES (7, 'Fat', 'Man', true, 2, 99, 'This is another address', 1569673923);

