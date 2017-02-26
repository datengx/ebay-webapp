
-- Count he number of users in the databases
SELECT COUNT(DISTINCT User.UserID) FROM User;


-- Count the number of items in New York
SELECT COUNT(Item.ItemID) FROM Item
INNER JOIN
User
ON Item.UserID = User.UserID
WHERE BINARY Location = 'New York';


-- Find number of autions belonging to exactly 4 cats
SELECT COUNT(*) FROM
( SELECT COUNT(*) AS CatCount FROM ItemCategory
GROUP BY ItemID) AS CatCountQuery WHERE CatCount = 4;

-- Find the ID(s) of current
SELECT Bid.ItemID FROM Bid
INNER JOIN
(SELECT Item.ItemID AS CurrentItem FROM Item
WHERE Ends > '2001-12-20 00:00:01') AS T
ON CurrentItem = Bid.ItemID
WHERE Amount = (SELECT MAX(Bid.Amount) FROM Bid);


-- Find the number of sellers with rating > 1000
SELECT COUNT(*) FROM SellerRate
WHERE Rating > 1000;

-- Find the number of users who are both sellers and bidders
SELECT COUNT(DISTINCT SellerRate.UserID) FROM SellerRate
INNER JOIN
BidderRate
ON SellerRate.UserID = BidderRate.UserID;

-- Find the number of categories that include at least one item with a 
-- bid of more than $100
SELECT COUNT(DISTINCT CategoryID)
FROM ItemCategory
INNER JOIN Bid
ON Bid.ItemID = ItemCategory.ItemID
WHERE Amount > 100;
