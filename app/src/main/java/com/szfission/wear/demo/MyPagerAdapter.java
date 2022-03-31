package com.szfission.wear.demo;

import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public  class MyPagerAdapter extends PagerAdapter {
    private List<View> mViewList;
    private List<String> titles;

    public MyPagerAdapter(List<View> mViewList,List<String> titles) {
        this.mViewList = mViewList;
        this.titles = titles;
    }

    @Override
    public int getCount() {
        return mViewList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(mViewList.get(position));
        return mViewList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewList.get(position));
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
