/* CS144
 *
 * Parser skeleton for processing item-???.xml files. Must be compiled in
 * JDK 1.5 or above.
 *
 * Instructions:
 *
 * This program processes all files passed on the command line (to parse
 * an entire diectory, type "java MyParser myFiles/*.xml" at the shell).
 *
 * At the point noted below, an individual XML file has been parsed into a
 * DOM Document node. You should fill in code to process the node. Java's
 * interface for the Document Object Model (DOM) is in package
 * org.w3c.dom. The documentation is available online at
 *
 * http://java.sun.com/j2se/1.5.0/docs/api/index.html
 *
 * A tutorial of Java's XML Parsing can be found at:
 *
 * http://java.sun.com/webservices/jaxp/
 *
 * Some auxiliary methods have been written for you. You may find them
 * useful.
 */

package edu.ucla.cs.cs144;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;
import javax.lang.model.element.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


class MyParser {

    static final String columnSeparator = "|*|";
    static DocumentBuilder builder;

    static final String[] typeName = {
	"none",
	"Element",
	"Attr",
	"Text",
	"CDATA",
	"EntityRef",
	"Entity",
	"ProcInstr",
	"Comment",
	"Document",
	"DocType",
	"DocFragment",
	"Notation",
    };

    static final String TAG_Item = "Item";
    static final String TAG_ItemID = "ItemID";
    static final String TAG_Name = "Name";
    static final String TAG_Category = "Category";
    static final String TAG_Seller = "Seller";
    static final String TAG_Bidder = "Bidder";
    static final String TAG_Bids = "Bids";
    static final String TAG_Bid = "Bid";
    static final String TAG_Rating = "Rating";
    static final String TAG_Country = "Country";
    static final String TAG_Location = "Location";
    static final String TAG_UserID = "UserID";
    static final String TAG_Started = "Started";
    static final String TAG_Ends = "Ends";
    static final String TAG_Description = "Description";
    static final String TAG_Time = "Time";
    static final String TAG_Amount = "Amount";
    static final String TAG_First_Bid = "First_Bid";
    static final String TAG_Buy_Price = "Buy_Price";
    static final String TAG_Currently = "Currently";

    static int category_count = 0;
    static HashMap<String, Integer> category_map = new HashMap<String, Integer>();
    static HashMap<String, String> bidders_map = new HashMap<String, String>();
    static HashMap<String, String> sellers_map = new HashMap<String, String>();
    static HashMap<String, String> users_map = new HashMap<String, String>();
    // XML to mySQL date formater
    static SimpleDateFormat xmlFormat = new SimpleDateFormat("MMM-dd-yy HH:mm:ss");
    static SimpleDateFormat mySQLFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static int mCurFileIndex;

    static class MyErrorHandler implements ErrorHandler {

        public void warning(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }

        public void error(SAXParseException exception)
        throws SAXException {
            fatalError(exception);
        }

        public void fatalError(SAXParseException exception)
        throws SAXException {
            exception.printStackTrace();
            System.out.println("There should be no errors " +
                               "in the supplied XML files.");
            System.exit(3);
        }

    }

    /* Non-recursive (NR) version of Node.getElementsByTagName(...)
     */
    static Element[] getElementsByTagNameNR(Element e, String tagName) {
        Vector< Element > elements = new Vector< Element >();
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
            {
                elements.add( (Element)child );
            }
            child = child.getNextSibling();
        }
        Element[] result = new Element[elements.size()];
        elements.copyInto(result);
        return result;
    }

    /* Returns the first subelement of e matching the given tagName, or
     * null if one does not exist. NR means Non-Recursive.
     */
    static Element getElementByTagNameNR(Element e, String tagName) {
        Node child = e.getFirstChild();
        while (child != null) {
            if (child instanceof Element && child.getNodeName().equals(tagName))
                return (Element) child;
            child = child.getNextSibling();
        }
        return null;
    }

    /* Returns the text associated with the given element (which must have
     * type #PCDATA) as child, or "" if it contains no text.
     */
    static String getElementText(Element e) {
        if (e.getChildNodes().getLength() == 1) {
            Text elementText = (Text) e.getFirstChild();
            return elementText.getNodeValue();
        }
        else
            return "";
    }

    /* Returns the text (#PCDATA) associated with the first subelement X
     * of e with the given tagName. If no such X exists or X contains no
     * text, "" is returned. NR means Non-Recursive.
     */
    static String getElementTextByTagNameNR(Element e, String tagName) {
        Element elem = getElementByTagNameNR(e, tagName);
        if (elem != null)
            return getElementText(elem);
        else
            return "";
    }

    /* Returns the amount (in XXXXX.xx format) denoted by a money-string
     * like $3,453.23. Returns the input if the input is an empty string.
     */
    static String strip(String money) {
        if (money.equals(""))
            return money;
        else {
            double am = 0.0;
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            try { am = nf.parse(money).doubleValue(); }
            catch (ParseException e) {
                System.out.println("This method should work for all " +
                                   "money values you find in our data.");
                System.exit(20);
            }
            nf.setGroupingUsed(false);
            return nf.format(am).substring(1);
        }
    }


    /**
    * Parse all the category belonging to an item. Put all the extracted
    * categories into an HashMap storing all the categories
    */
    static void createCategoriesTable(Element item, BufferedWriter writer) throws IOException {
            Element [] cats = getElementsByTagNameNR(item, TAG_Category);

            // Put all the category in a HashMap. At the same time, eliminate duplicate
            for (int i = 0; i < cats.length; i++) {
                    String cat_name = getElementText(cats[i]);
                    if (!category_map.containsKey(cat_name)) {
                            // if the category has not been put in the category yet
                            category_map.put(cat_name, new Integer(category_count));
                            writer.write(category_count + columnSeparator + cat_name);
                            writer.newLine();
                            category_count++; // increment the count
                    }
            }
    }

    /**
    * Parse all the item categories belonging to an item
    *
    */
    static void createItemCategoriesTable(Element item, BufferedWriter writer) throws IOException {
            String itemID = item.getAttribute(TAG_ItemID);
            Element [] cats = getElementsByTagNameNR(item, TAG_Category);

            for (int i = 0; i < cats.length; i++) {
                    String cat_name = getElementText(cats[i]);
                    Integer key = category_map.get(cat_name);
                    writer.write(itemID + columnSeparator + key);
                    writer.newLine();
            }
    }

    /**
    * Parse all the users (bidders/seller) within an item element
    *
    */
    static void createUsersTable(Element item, BufferedWriter sellersWriter, BufferedWriter biddersWriter, BufferedWriter usersWriter) throws IOException {
            Element seller = getElementByTagNameNR(item, TAG_Seller);
            String seller_userID = seller.getAttribute(TAG_UserID);
            String seller_rating = seller.getAttribute(TAG_Rating);
            String seller_location = getElementTextByTagNameNR(item, TAG_Location);
            String seller_country = getElementTextByTagNameNR(item, TAG_Country);

            // deal with duplicate
            if (!sellers_map.containsKey(seller_userID)) {
                    sellers_map.put(seller_userID, seller_userID);
                    sellersWriter.write(seller_userID + columnSeparator
                                      + seller_rating);
                    sellersWriter.newLine();
            }
            if (!users_map.containsKey(seller_userID)) {
                users_map.put(seller_userID, seller_userID);
                usersWriter.write(seller_userID + columnSeparator
                                + seller_location + columnSeparator
                                + seller_country);
                usersWriter.newLine();
            }

            Element [] bids = getElementsByTagNameNR(getElementByTagNameNR(item, TAG_Bids), TAG_Bid);

            for (int i = 0; i < bids.length; i++) {
                    Element bidder = getElementByTagNameNR(bids[i], TAG_Bidder);
                    String bidder_userID = bidder.getAttribute(TAG_UserID);
                    String bidder_rating = bidder.getAttribute(TAG_Rating);
                    String bidder_location = getElementTextByTagNameNR(bidder, TAG_Location);
                    String bidder_country = getElementTextByTagNameNR(bidder, TAG_Country);

                    if (!bidders_map.containsKey(bidder_userID)) {
                            bidders_map.put(bidder_userID, bidder_userID);
                            biddersWriter.write(bidder_userID + columnSeparator
                                              + bidder_rating);
                            biddersWriter.newLine();
                    }
                    if (!users_map.containsKey(bidder_userID)) {
                        users_map.put(bidder_userID, bidder_userID);
                        usersWriter.write(bidder_userID + columnSeparator
                                        + bidder_location + columnSeparator
                                        + bidder_country);
                        usersWriter.newLine();
                    }
            }

    }

    /**
    * Parse Bids in the item
    *
    */
    static void createBidsTable(Element item, BufferedWriter writer) throws IOException, ParseException {
            String itemID = item.getAttribute(TAG_ItemID);
            Element bids[] = getElementsByTagNameNR(getElementByTagNameNR(item, TAG_Bids), TAG_Bid);

            for (int i = 0; i < bids.length; i++) {
                    Element bidder = getElementByTagNameNR(bids[i], TAG_Bidder);
                    String userID = bidder.getAttribute(TAG_UserID);
                    // parse into mySQL acceptable date format
                    String time = mySQLFormat.format(xmlFormat.parse(getElementTextByTagNameNR(bids[i], TAG_Time)));
                    String amount = strip(getElementTextByTagNameNR(bids[i], TAG_Amount));

                    writer.write(itemID + columnSeparator
                               + userID + columnSeparator
                               + time + columnSeparator
                               + amount);
                    writer.newLine();
            }
    }

    /**
    * Parse Items in the current item element
    *
    */
    static void createItemsTable(Element item, BufferedWriter writer) throws IOException, ParseException {
            String itemID = item.getAttribute(TAG_ItemID);
            String name = getElementTextByTagNameNR(item, TAG_Name);
            String seller_userID = getElementByTagNameNR(item, TAG_Seller).getAttribute(TAG_UserID);
            String started = mySQLFormat.format(xmlFormat.parse(getElementTextByTagNameNR(item, TAG_Started)));
            String ends = mySQLFormat.format(xmlFormat.parse(getElementTextByTagNameNR(item, TAG_Ends)));

            String first_bid = strip(getElementTextByTagNameNR(item, TAG_First_Bid));
            String buy_price = strip(getElementTextByTagNameNR(item, TAG_Buy_Price));
            String currently = strip(getElementTextByTagNameNR(item, TAG_Currently));

            // modification since project2 - add latitude and longitude to the tables
            Element location = getElementByTagNameNR(item, TAG_Location);
            String latitude = "999"; // initialize to absurd value that could never occur
            String longitude = "999";
            if ((!location.getAttribute("Latitude").equals("")) && (!location.getAttribute("Longitude").equals(""))) {
              latitude = location.getAttribute("Latitude");
              longitude = location.getAttribute("Longitude");
            }

            String description = getElementTextByTagNameNR(item, TAG_Description);
            // If the length of the string is larger than 4000,
            // crop it to 4000
            if (description.length() > 4000) {
                description = description.substring(0, 4000);
            }
            writer.write(
                        itemID + columnSeparator
                      + name   + columnSeparator
                      + seller_userID + columnSeparator
                      + started + columnSeparator
                      + ends + columnSeparator
                      + first_bid + columnSeparator
                      + buy_price + columnSeparator
                      + currently + columnSeparator
                      + latitude + columnSeparator
                      + longitude + columnSeparator
                      + description
            );
            writer.newLine();
    }

    /* Process one items-???.xml file.
     */
    static void processFile(File xmlFile) throws IOException, ParseException {
        // declaring doc object
        Document doc = null;
        try {
            // initialize doc object
            doc = builder.parse(xmlFile);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(3);
        }
        catch (SAXException e) {
            System.out.println("Parsing error on file " + xmlFile);
            System.out.println("  (not supposed to happen with supplied XML files)");
            e.printStackTrace();
            System.exit(3);
        }

        /* At this point 'doc' contains a DOM representation of an 'Items' XML
         * file. Use doc.getDocumentElement() to get the root Element. */
        System.out.println("Successfully parsed - " + xmlFile);

        /* Fill in code here (you will probably need to write auxiliary
            methods). */
        // Start playing around with the doc object
        Element root = doc.getDocumentElement();
        Element [] items = getElementsByTagNameNR(root, TAG_Item);

        File itemsFile = new File("items.dat");
        File sellersFile = new File("sellersRate.dat");
        File biddersFile = new File("biddersRate.dat");
        File usersFile = new File("users.dat");
        File itemCategoriesFile = new File("itemCategories.dat");
        File categoriesFile = new File("categories.dat");
        File bidsFile = new File("bids.dat");
        boolean append_res;
        // append results to the end of file if not the first file to parse
        if (mCurFileIndex != 0) {
            append_res = true;
        } else {
            append_res = false;
        }
        BufferedWriter itemsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(itemsFile, append_res)));
        BufferedWriter sellersWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sellersFile, append_res)));
        BufferedWriter biddersWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(biddersFile, append_res)));
        BufferedWriter usersWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(usersFile, append_res)));
        BufferedWriter itemCategoriesWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(itemCategoriesFile, append_res)));
        BufferedWriter categoriesWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(categoriesFile, append_res)));
        BufferedWriter bidsWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bidsFile, append_res)));



        // Traverse through all the
        for (int i = 0; i < items.length; i++) {
            createCategoriesTable(items[i], categoriesWriter);
            createItemCategoriesTable(items[i], itemCategoriesWriter);
            createUsersTable(items[i], sellersWriter, biddersWriter, usersWriter);
            createBidsTable(items[i], bidsWriter);
            createItemsTable(items[i], itemsWriter);
        }

//        for (Map.Entry<String, Integer> entry : category_map.entrySet()) {
//            System.out.println(entry.getKey());
//        }

        // Clean up
        itemsWriter.close();
        sellersWriter.close();
        biddersWriter.close();
        usersWriter.close();
        itemCategoriesWriter.close();
        categoriesWriter.close();
        bidsWriter.close();


        /**************************************************************/

    }



    public static void main (String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java MyParser [file] [file] ...");
            System.exit(1);
        }

        /* Initialize parser. */
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);
            builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
        }
        catch (FactoryConfigurationError e) {
            System.out.println("unable to get a document builder factory");
            System.exit(2);
        }
        catch (ParserConfigurationException e) {
            System.out.println("parser was unable to be configured");
            System.exit(2);
        }

        try {
            /* Process all files listed on command line. */
            for (int i = 0; i < args.length; i++) {
                mCurFileIndex = i;
                File currentFile = new File(args[i]);
                processFile(currentFile);
            }
        }
        catch (IOException e) {
            System.out.println("Error write to files.");
        }
        catch (ParseException e) {
            System.out.println("Error parsing files.");
        }
    }
}
