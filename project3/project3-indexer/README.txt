This example contains a simple utility class to simplify opening database
connections in Java applications, such as the one you will write to build
your Lucene index.

To build and run the sample code, use the "run" ant target inside
the directory with build.xml by typing "ant run".



Index Design:
Since the keyword search requires us to perform searching over the union of
Name, Category, Description so I decided to make StringField's for Name and
ID in the index, which will be used to access the index. The TextField that I will create to
perform search on will include Name, Category and the Description for the Item.
