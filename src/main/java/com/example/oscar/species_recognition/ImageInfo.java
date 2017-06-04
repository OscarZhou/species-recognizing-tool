package com.example.oscar.species_recognition;

import android.graphics.Bitmap;

/**
 * Created by oscar on 6/4/2017.
 */

public class ImageInfo {

    Bitmap bitmap;
    String title;
    String description;

    public ImageInfo()
    {


    }

    public ImageInfo(Bitmap bitmap, String title, String description){
        this.bitmap = bitmap;
        this.title = title;
        this.description = description;

    }

    public void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;

    }

    public Bitmap getBitmap()
    {

        return this.bitmap;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
    public String getTitle()
    {
        return this.title;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return this.description;
    }
}
