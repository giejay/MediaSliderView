package com.zeuskartik.mediaslider;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class SliderItem implements Parcelable {
    private final String id;
    private final String url;
    private final SliderItemType type;
    private final String description;
    private final String subtitle;
    private final Date date;
    private final String thumbnailUrl;

    public SliderItem(String id, String url, SliderItemType type, String description, String subtitle, Date date, String thumbnailUrl) {
        this.id = id;
        this.url = url;
        this.type = type;
        this.description = description;
        this.subtitle = subtitle;
        this.date = date;
        this.thumbnailUrl = thumbnailUrl;
    }

    protected SliderItem(Parcel in) {
        id = in.readString();
        url = in.readString();
        type = SliderItemType.valueOf(in.readString());
        description = in.readString();
        subtitle = in.readString();
        date = (java.util.Date) in.readSerializable();
        thumbnailUrl = in.readString();
    }

    public static final Creator<SliderItem> CREATOR = new Creator<SliderItem>() {
        @Override
        public SliderItem createFromParcel(Parcel in) {
            return new SliderItem(in);
        }

        @Override
        public SliderItem[] newArray(int size) {
            return new SliderItem[size];
        }
    };

    public String getUrl() {
        return url;
    }

    public SliderItemType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Date getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(url);
        dest.writeString(type.toString());
        dest.writeString(description);
        dest.writeString(subtitle);
        dest.writeSerializable(date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SliderItem that = (SliderItem) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
