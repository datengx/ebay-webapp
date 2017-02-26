-- Create spatial index


CREATE TABLE ItemLocation (
  ItemID INTEGER NOT NULL,
  Latitude DECIMAL(10,6) NOT NULL,
  Longitude DECIMAL(10,6) NOT NULL,
  PRIMARY KEY(ItemID),
  FOREIGN KEY(ItemID) REFERENCES Item(ItemID)
) ENGINE=MyISAM;

-- Insert data into the new table
INSERT INTO ItemLocation
(ItemID, Latitude, Longitude)
SELECT ItemID, Latitude, Longitude
FROM Item;

-- create spatial index
ALTER TABLE ItemLocation ADD Coord POINT;
UPDATE ItemLocation SET Coord=POINT(Latitude, Longitude);
ALTER TABLE ItemLocation MODIFY Coord POINT NOT NULL;
CREATE SPATIAL INDEX SpatialIndex ON ItemLocation(Coord);
