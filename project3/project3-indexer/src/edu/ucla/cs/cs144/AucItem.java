package edu.ucla.cs.cs144;

import java.io.*;
import java.text.*;
import java.util.*;

public class AucItem {

  String mID;
  String mName;
  String mDesc;
  List<String> mCategoryList = new ArrayList<String>();

  public AucItem () {}

  public AucItem (String id, String name) {
    mID = id;
    mName = name;
  }

  public void addCategory(String category) {
    mCategoryList.add(category);
  }

  /**
   * Return all the categories that the item belongs in a single String.
   * The categories are separated by spaces
   * @return String contains all the categories of the item
   */
  public String getCategory() {
    String rtn = "";
    for (String cat : mCategoryList) {
      rtn = rtn + cat + " ";
    }
    return rtn;
  }

  public void setDesc(String desc) {
    mDesc = desc;
  }

  public String getDesc() {
    return mDesc;
  }

  public String toString() {
    String rtn = mID + "|" + mName + "|";
    for (String cat : mCategoryList) {
      rtn = rtn + cat + " ";
    }
    return rtn;
  }

  public String getID() {
    return mID;
  }

  public String getName() {
    return mName;
  }

}
