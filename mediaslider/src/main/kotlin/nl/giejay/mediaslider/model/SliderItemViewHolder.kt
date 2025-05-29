package com.zeuskartik.mediaslider;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SliderItemViewHolder(SliderItem mainItem, @Nullable SliderItem secondaryItem) {

    public SliderItemViewHolder(SliderItem mainItem) {
        this(mainItem, null);
    }

    public boolean hasSecondaryItem(){
        return secondaryItem != null;
    }

    public SliderItemType getType(){
        return mainItem.getType();
    }

    public String getUrl(){
        return mainItem().getUrl();
    }

    public String getDescriptionRight(){
        return secondaryItem != null ? secondaryItem.getDescription() : mainItem.getDescription();
    }

    public String getDescriptionLeft(){
        return mainItem.getDescription();
    }

    public String getSubtitleLeft(){
        return mainItem().getSubtitle();
    }

    public String getSubtitleRight(){
        return secondaryItem != null ? secondaryItem.getSubtitle() : mainItem().getSubtitle();
    }

    public Date getDateLeft(){
        return mainItem.getDate();
    }

    public Date getDateRight(){
        return secondaryItem != null ? secondaryItem.getDate() : mainItem.getDate();
    }

    public String getThumbnailUrl(){
        return mainItem.getThumbnailUrl();
    }

    public List<String> ids(){
        return Stream.of(mainItem.getId(), secondaryItem != null ? secondaryItem.getId() : null).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
