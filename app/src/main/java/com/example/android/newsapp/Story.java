package com.example.android.newsapp;

import android.os.Parcel;
import android.os.Parcelable;

public class Story implements Parcelable {

    public static final Creator<Story> CREATOR = new Creator<Story>() {
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        public Story[] newArray(int size) {
            return new Story[size];
        }
    };
    private String mTitle;
    private String mSection;
    private String mDate;
    private String mUrl;

    public Story(String title, String section, String date, String url) {
        mTitle = title;
        mSection = section;
        mDate = date;
        mUrl = url;
    }

    private Story(Parcel in) {
        mTitle = in.readString();
        mSection = in.readString();
        mDate = in.readString();
        mUrl = in.readString();
    }

    public String getSection() {
        return mSection;
    }

    public String getDate() {
        return mDate;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mUrl;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTitle);
        out.writeString(mSection);
        out.writeString(mDate);
        out.writeString(mUrl);
    }

}
