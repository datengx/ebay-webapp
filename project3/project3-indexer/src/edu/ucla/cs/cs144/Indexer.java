package edu.ucla.cs.cs144;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import java.text.*;
import java.util.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

    HashMap<String, AucItem> itemsMap = new HashMap<String, AucItem>();

    /** Creates a new instance of Indexer */
    public Indexer() {
    }

    private IndexWriter indexWriter = null;

    public IndexWriter getIndexWriter(boolean create) throws IOException {
      if (indexWriter == null) {
          Directory indexDir = FSDirectory.open(new File("/var/lib/lucene/index1"));
          IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_2, new StandardAnalyzer());
          indexWriter = new IndexWriter(indexDir, config);
      }
      return indexWriter;
    }


    public void closeIndexWriter() throws IOException {
      if (indexWriter != null) {
        indexWriter.close();
      }
    }

    public void indexItem(AucItem item) throws IOException {
      Document doc = new Document();
      doc.add(new StringField("id", item.getID(), Field.Store.YES));
      doc.add(new StringField("name", item.getName(), Field.Store.YES));
      String fullSearchableText = item.getName() + " " + item.getCategory() + " " + item.getDesc();
      doc.add(new TextField("content", fullSearchableText, Field.Store.NO));
      indexWriter.addDocument(doc);
    }

    public void rebuildIndexes() throws IOException {

        Connection conn = null;

        // create a connection to the database to retrieve Items from MySQL
	try {
	    conn = DbManager.getConnection(true);
	} catch (SQLException ex) {
	    System.out.println(ex);
	}


	/*
	 * Add your code here to retrieve Items using the connection
	 * and add corresponding entries to your Lucene inverted indexes.
         *
         * You will have to use JDBC API to retrieve MySQL data from Java.
         * Read our tutorial on JDBC if you do not know how to use JDBC.
         *
         * You will also have to use Lucene IndexWriter and Document
         * classes to create an index and populate it with Items data.
         * Read our tutorial on Lucene as well if you don't know how.
         *
         * As part of this development, you may want to add
         * new methods and create additional Java classes.
         * If you create new classes, make sure that
         * the classes become part of "edu.ucla.cs.cs144" package
         * and place your class source files at src/edu/ucla/cs/cs144/.
	 *
	 */
   try {
     // create a statement used to communicate with the database
     Statement s = conn.createStatement();

     // execute query
     // return ItemID, Names, Categories for all of the item
     ResultSet rs = s.executeQuery("SELECT Item.ItemID, Name, Category FROM Item"
                                + " INNER JOIN ItemCategory"
                                + " ON Item.ItemID = ItemCategory.ItemID"
                                + " INNER JOIN Category"
                                + " ON ItemCategory.CategoryID = Category.CategoryID");



     // Loop through the results
     while (rs.next()) {
       String itemID = "" + rs.getInt("ItemID");
       String name = rs.getString("Name");
       String category = rs.getString("Category");
       // check if the item is already in the map
       // if it does, then we only need to add the
       // new category
       if (itemsMap.containsKey(itemID)) {
         AucItem item = itemsMap.get(itemID);
         // assuming that the same category would not appear
         // more than once
         item.addCategory(category);
       } else {
         AucItem item = new AucItem(itemID, name);
         item.addCategory(category);
         // put the new item in the map
         itemsMap.put(itemID, item);
       }
     }

     rs = s.executeQuery(
       "SELECT ItemID, Description FROM Item"
     );

     while (rs.next()) {
       String itemID = "" + rs.getInt("ItemID");
       String desc = rs.getString("Description");
       AucItem item = itemsMap.get(itemID);
       item.setDesc(desc);
       // Debug print items
       // System.out.println(item.toString());
     }

     // System.out.println("Found " + itemsMap.size() + " items");


     // cleanup
     rs.close(); // close resultset
     s.close(); // close statement
   } catch (SQLException ex) {
     System.out.println(ex);
   }


  // close the database connection
	try {
	  conn.close();
	} catch (SQLException ex) {
	  System.out.println(ex);
	}

      getIndexWriter(true);
      // Start indexing
      for (AucItem item : itemsMap.values()) {
        indexItem(item);
      }
      closeIndexWriter();
    }

    public static void main(String args[]) {
      try {
        Indexer idx = new Indexer();
        idx.rebuildIndexes();
      } catch (IOException ex) {
        System.out.println(ex);
      }
    }
}
