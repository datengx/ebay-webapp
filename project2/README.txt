CS144 Project 2
Name: Da Teng  ID: 804592061

1. Relations:

- Item(ItemID, Name, UserID, Started, Ends, FirstBid, BuyPrice, Currently, Latitude, Longitude, Description) primary keys: ItemID
- User(UserID, Location, Country) primary key: UserID
- SellerRate(UserID, Rating) primary keys: UserID
- BidderRate(UserID, Rating) primary keys: UserID
- ItemCategory(ItemID, CategoryID) primary keys: ItemID
- Category(CategoryID, Category) primary keys: CategoryID
- Bid(ItemID, UserID, Time, Amount) primary keys: (ItemID, UserID)

I distinguish Seller and Bidder since a user can be either a seller and a bidder. He/She has different rating
as different roles.

2.

All of the completely nontrivial dependencies of the attributes are on keys.

3/4. All of the relations are BCNF and 4NF
