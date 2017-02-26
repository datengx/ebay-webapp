package edu.ucla.cs.cs144;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.lang.Math.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.ucla.cs.cs144.DbManager;
import edu.ucla.cs.cs144.SearchRegion;
import edu.ucla.cs.cs144.SearchResult;

public class AuctionSearch implements IAuctionSearch {

	/*
         * You will probably have to use JDBC to access MySQL data
         * Lucene IndexSearcher class to lookup Lucene index.
         * Read the corresponding tutorial to learn about how to use these.
         *
	 * You may create helper functions or classes to simplify writing these
	 * methods. Make sure that your helper functions are not public,
         * so that they are not exposed to outside of this class.
         *
         * Any new classes that you create should be part of
         * edu.ucla.cs.cs144 package and their source files should be
         * placed at src/edu/ucla/cs/cs144.
         *
         */
	private IndexSearcher searcher = null;
	private QueryParser parser = null;
	private final String index_path = "/var/lib/lucene/index1";
	private HashMap<String, String> itemMap = new HashMap<String, String>();

	public SearchResult[] basicSearch(String query, int numResultsToSkip,
			int numResultsToReturn) {
		SearchResult[] searchResults = new SearchResult[0];
		try {
			searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(index_path))));
			parser = new QueryParser("content", new StandardAnalyzer());
			// construct the query
			Query queryObj = parser.parse(query);

			TopDocs topDocs = searcher.search(queryObj, numResultsToSkip+numResultsToReturn);

			ScoreDoc[] hits = topDocs.scoreDocs;
			// System.out.println("Found " + hits.length + " matches.");
			// calculate the number of results that are with in the range of request
			int len = Math.min(hits.length-numResultsToSkip, numResultsToReturn);

			// initialize the return array
			searchResults = new SearchResult[len];
			int count = 0;
			for (int i = numResultsToSkip; i < numResultsToSkip+len; i++) {
					Document doc = searcher.doc(hits[i].doc);
					String id = doc.get("id");
					String name = doc.get("name");

					// add new search object to the return array
					searchResults[count] = new SearchResult(id, name);
					count++;
			}
		} catch (IOException ex) {
			System.out.println(ex);
		} catch (ParseException ex) {
			System.out.println(ex);
		}

		return searchResults;
	}

	public SearchResult[] spatialSearch(String query, SearchRegion region,
			int numResultsToSkip, int numResultsToReturn) {
		SearchResult[] searchResults;
		List<SearchResult> resultList = new ArrayList<SearchResult>();

		Connection conn = null;
		// Establish connection
		try {
			conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
			System.out.println(ex);
		}

		// spatial query from region information
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM ItemLocation WHERE MBRContains(GeomFromText('"
																	 +"Polygon(("
																	 +region.getLx() + " " + region.getLy() + ", " // lower left
																	 +region.getLx() + " " + region.getRy() + ", " // upper left
																	 +region.getRx() + " " + region.getRy() + ", " // upper right
																	 +region.getRx() + " " + region.getLy() + ", " // lower right
																	 +region.getLx() + " " + region.getLy()        // lower left, loop closure
																	 +"))"
																	 +"'), Coord)"
																		);
			int count = 0;
			while (rs.next()) {
				String itemID = "" + rs.getInt("ItemID");
				itemMap.put(itemID, itemID);
				count++;
			}
			// System.out.println("Spatial search found (before map) " + count + " results");
			// System.out.println("Spatial search found " + itemMap.size() + " results");

			// perform search based on keyword
			SearchResult[] basicResults = this.basicSearch(query, 0, 20000);

			// find intersection of the spatial search results
			for (SearchResult result : basicResults) {
				if (itemMap.containsKey(result.getItemId())) {
					resultList.add(result);
				}
			}
			// System.out.println("Both constraints return " + resultList.size() + " results.");

			rs.close();
			s.close();
		} catch (SQLException ex) {
			System.out.println(ex);
		}

		// Close connection
		try {
		  conn.close();
		} catch (SQLException ex) {
		  System.out.println(ex);
		}
		searchResults = new SearchResult[resultList.size()];
		resultList.toArray(searchResults);
		return searchResults;
	}







	public String getXMLDataForItemId(String itemId) {
		// String to construct xml
		String xmlString = "";
		Connection conn = null;
		// Establish connection
		try {
			conn = DbManager.getConnection(true);
		} catch (SQLException ex) {
			System.out.println(ex);
		}

		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM Item WHERE ItemID=" + itemId);

			// if the result is no empty
			if (rs.next()) {
				String name = escapeChars(rs.getString("Name"));
				String id = escapeChars(rs.getString("ItemID"));
				String user_id = escapeChars(rs.getString("UserID"));
				String currently = String.format("%,.2f", Float.valueOf(rs.getString("Currently")));
				String first_bid = String.format("%,.2f", Float.valueOf(rs.getString("First_Bid")));
				String buy_price = rs.getString("BuyPrice");
				String desc = escapeChars(rs.getString("Description"));



				String started = rs.getString("Started");
				String ends = rs.getString("Ends");
				try {
					SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					SimpleDateFormat formatter = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
					Date dd = parser.parse(started);
				  started = formatter.format(dd);
					dd = parser.parse(ends);
					ends = formatter.format(dd);
				} catch (Exception e) {
				 	System.out.println(e);
			 	}

				// Retrieve location information
				ResultSet rs_loc = s.executeQuery("SELECT Location, Country FROM Item"
				 																 +" INNER JOIN User"
																				 +" ON"
																				 +" Item.UserID = User.UserID"
																				 +" WHERE User.UserID = '" + user_id + "'");

				String location = "";
				String country = "";
				String latitude = "";
				String longitude = "";
				if (rs_loc.next()) {
					location = escapeChars(rs_loc.getString("Location"));
					country = escapeChars(rs_loc.getString("Country"));
				}
				rs_loc = s.executeQuery("SELECT Latitude, Longitude FROM ItemLocation"
															+ " WHERE ItemID = '" + id + "'");
				if (rs_loc.next()) {
					latitude = rs_loc.getString("Latitude");
					longitude = rs_loc.getString("Longitude");
					if (Float.valueOf(latitude) == 999.0) {
						latitude = "";
					}
					if (Float.valueOf(longitude) == 999.0) {
						longitude = "";
					}
				}
				rs_loc.close();





				// Retrieve categories information
				ResultSet rs_cat = s.executeQuery("SELECT Category FROM ItemCategory"
																				 + " INNER JOIN Category"
																				 + " ON"
																				 + " ItemCategory.CategoryID = Category.CategoryID"
																				 + " WHERE ItemCategory.ItemID = " + id);
        String entry_category = "";
				while (rs_cat.next()) {
					entry_category = entry_category + "  <Category>"
																				  + escapeChars(rs_cat.getString("Category"))
																					+ "</Category>\n";
				}
				rs_cat.close();




				// Construct Item/ItemID
				String entry_item =      "<Item ItemID=\"" + id + "\">\n";




				// Construct Name, Location, Country, Currently, First_Bid, Buy_Price*
				String entry_name =      "  <Name>" + name + "</Name>\n";
				String entry_location = "";
				if (latitude.equals("") || longitude.equals("")) {
					entry_location =       "  <Location>" + location + "</Location>\n";
				} else {
					entry_location =       "  <Location Latitude=\"" + latitude + "\" Longitude=\""
																                           + longitude + "\">"
																                           + location + "</Location>\n";
				}
				String entry_country =   "  <Country>" + country + "</Country>\n";
				String entry_currently = "  <Currently>$" + currently + "</Currently>\n";
				String entry_first_bid = "  <First_Bid>$" + first_bid + "</First_Bid>\n";
				String entry_buy_price = "";
				String entry_started =   "  <Started>" + started + "</Started>\n";
				String entry_ends =      "  <Ends>" + ends + "</Ends>\n";
				// Assuming that no one will set a buy price
				// of 0.0
				if (Float.valueOf(buy_price) == 0.0) {
					// System.out.println("Buy price does not exists");
				} else {
					entry_buy_price =      "  <Buy_Price>$" + String.format("%,.2f", Float.valueOf(buy_price)) + "</Buy_Price>\n";
				}



				/**
				 *	Construct the Bid information
				 *
				 */
				ResultSet rs_bid = s.executeQuery("SELECT * FROM Bid"
				                           + " INNER JOIN User"
																	 + " ON"
																	 + " Bid.UserID = User.UserID"
																	 + " INNER JOIN BidderRate"
																	 + " ON"
																	 + " User.UserID = BidderRate.UserID"
																	 + " WHERE Bid.ItemID = " + id);
				String entry_bid = "";
				int bid_count = 0;
				while (rs_bid.next()) {
					bid_count++;
					String rating = rs_bid.getString("Rating");
					String bidder_id = rs_bid.getString("UserID");
					String item_location = escapeChars(rs_bid.getString("Location"));
					String item_country = escapeChars(rs_bid.getString("Country"));
					String bid_time = rs_bid.getString("Time");
					String bid_amount = rs_bid.getString("Amount");
					try {
						SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						SimpleDateFormat formatter = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
						Date dd = parser.parse(bid_time);
					  bid_time = formatter.format(dd);
					} catch (Exception e) {
					 	System.out.println(e);
				 	}
					entry_bid = entry_bid + "    <Bid>\n"
																+ "      <Bidder Rating=\"" + rating + "\" UserID=\"" + bidder_id + "\">\n"
																+ "        <Location>" + item_location + "</Location>\n"
																+ "        <Country>" + item_country + "</Country>\n"
																+ "      </Bidder>\n"
																+ "      <Time>" + bid_time + "</Time>\n"
																+ "      <Amount>$" + String.format("%,.2f", Float.valueOf(bid_amount)) + "</Amount>\n"
																+ "    </Bid>\n";
				}
				if (bid_count == 0) {
					entry_bid = "  </Bids>\n";
				} else {
					entry_bid = "  <Bids>\n" + entry_bid
										+ "  </Bids>\n";
				}
				String entry_num_bid = "  <Number_of_Bids>" + bid_count + "</Number_of_Bids>\n";
				rs_bid.close();


				/**
				 * seller rating information
				 *
				 */
				ResultSet rs_seller = s.executeQuery("SELECT Rating FROM SellerRate"
																							 + " WHERE SellerRate.UserID = \'" + user_id + "\'");
			  String entry_seller = "";
				if (rs_seller.next()) {
					entry_seller = "  <Seller Rating=\"" + rs_seller.getString("Rating") + "\" UserID="
															+ "\"" + user_id + "\" />\n";
				}

				rs_seller.close();
				String entry_desc = "";
				// Description
				if (!desc.equals("")) {
					entry_desc = "  <Description>" + desc + "</Description>\n";
				} else {
					entry_desc = "  </Description>\n";
				}

				/**
				 * construct the final xml String
				 *
				 */
				xmlString = entry_item
									+ entry_name
									+ entry_category
									+ entry_currently
									+ entry_first_bid
									+ entry_buy_price
									+ entry_num_bid
									+ entry_bid
									+ entry_location
									+ entry_country
									+ entry_started
									+ entry_ends
									+ entry_seller
									+ entry_desc
								  + "</Item>\n";
			}


			rs.close();
			s.close();
		} catch (SQLException ex) {
			System.out.println(ex);
		}


		// Close connection
		try {
		  conn.close();
		} catch (SQLException ex) {
		  System.out.println(ex);
		}
		return xmlString;
	}

	/**
	 * Helper function to escape characters in the xml file
	 * @param input input String
	 * @return String with 5 special characters escaped
	 */
	private String escapeChars(String input) {
		String rtn = "";
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			// For XML there are only five characters to escape according to thread
			// http://stackoverflow.com/questions/1091945/what-characters-do-i-need-to-escape-in-xml-documents
			switch (c) {
				case '\"':
					rtn += "&quot;";
					break;
				case '\'':
					rtn += "&apos;";
					break;
				case '<':
					rtn += "&lt;";
					break;
				case '>':
					rtn += "&gt;";
					break;
				case '&':
					rtn += "&amp;";
					break;
				default:
					rtn += c;
					break;
			}
		}
		return rtn;
	}

	public String echo(String message) {
		return message;
	}

}
