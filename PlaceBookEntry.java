package eecs40.placebook;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by stefancao on 5/31/15.
 */

public class PlaceBookEntry{
    private int id;
    private String name;
    private String description;
    private String date;
    private Bitmap image;

    public PlaceBookEntry(int id, String name, String description, String date, Bitmap image){
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.image = image;
    }

    public int getID(){
        return id;
    }

    public String getname(){
        return name;
    }

    public String getDescription(){
        return description;
    }

    public String getDate(){
        return date;
    }

    public Bitmap getImage(){
        return image;
    }

    public void setID(int id){
        this.id = id;
    }

    public void setname(String name){
        this.name = name;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public void setDate(String date){
        this.date = date;
    }

    public void setImage(Bitmap image){
        this.image = image;
    }



}


