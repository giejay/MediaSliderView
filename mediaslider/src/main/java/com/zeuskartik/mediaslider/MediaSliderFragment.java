package com.zeuskartik.mediaslider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import java.util.List;

public class MediaSliderFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return new MediaSliderView(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.requestFocus();
    }

    @Override
    public void onDestroyView() {
        MediaSliderView view = getView();
        if(view != null) {
            view.onDestroy();
        }
        super.onDestroyView();
    }

    @Nullable
    public MediaSliderView getView() {
        return (MediaSliderView) super.getView();
    }

    public void loadMediaSliderView(final MediaSliderConfiguration config, final List<SliderItem> items) {
        getView().loadMediaSliderView(config, items);
    }

    public void setDefaultExoFactory(DefaultHttpDataSource.Factory defaultExoFactory) {
        getView().setDefaultExoFactory(defaultExoFactory);
    }
}
