package com.zibuyuqing.roundcorner.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zibuyuqing.roundcorner.R;
import com.zibuyuqing.roundcorner.model.bean.AppInfo;
import com.zibuyuqing.roundcorner.model.db.AppInfoDaoOpe;

/**
 * Created by xijun.wang on 2017/6/22.
 */

public class AllAppsGridAdapter extends RecyclerView.Adapter {
    private Context mContext;
    private List<AppInfo> mAllApps;
    private LayoutInflater mInflate;
    private static final String TAG = "AllAppsGridAdapter";
    private ArrayMap<AppInfo, Integer> mChangedInfos = new ArrayMap<>();
    public static final Comparator<AppInfo> APPS_COMPARATOR = new Comparator<AppInfo>() {
        @Override
        public int compare(AppInfo one, AppInfo other) {
            return other.getEnableState() - one.getEnableState();
        }
    };
    public AllAppsGridAdapter(Context context, List<AppInfo> infos) {
        mContext = context;
        mInflate = LayoutInflater.from(context);
        initData(infos);
    }

    private void initData(List<AppInfo> infos) {
        Iterator iterator = infos.iterator();
        mAllApps = new ArrayList<>();
        AppInfo info;
        while (iterator.hasNext()) {
            info = (AppInfo) iterator.next();
            mAllApps.add(info);
        }
        Collections.sort(mAllApps,APPS_COMPARATOR);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflate.inflate(R.layout.layout_app_item, parent, false);
        ItemViewHolder viewHolder = new ItemViewHolder(view);
        viewHolder.appIcon = (ImageView) view.findViewById(R.id.iv_app_icon);
        viewHolder.appName = (TextView) view.findViewById(R.id.tv_app_name);
        viewHolder.appSelectedFlag = (ImageView) view.findViewById(R.id.iv_app_selected);
        viewHolder.appSelectedFlag.setVisibility(View.VISIBLE);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final ItemViewHolder viewHolder = (ItemViewHolder) holder;
        final AppInfo info = mAllApps.get(position);

        if (mChangedInfos.containsKey(info)) {
            viewHolder.verifySelectState(mChangedInfos.get(info));
        } else {
            viewHolder.verifySelectState(info.enableState);
        }

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewHolder.select == 1) {
                    viewHolder.select(0);
                } else {
                    viewHolder.select(1);
                }
                onItemChanged(info, viewHolder.select, info.enableState);
            }
        });
        // viewHolder.appIcon.setImageBitmap();
        viewHolder.appName.setText(info.title);
    }

    private void onItemChanged(AppInfo appInfo, int enable, int originEnableState) {
        if (enable != originEnableState) {
            mChangedInfos.put(appInfo, enable);
        } else {
            if (!mChangedInfos.isEmpty() && mChangedInfos.containsKey(appInfo)) {
                mChangedInfos.remove(appInfo);
                Log.i(TAG, "onItemChanged hidden state is not changed,should not update ,info =:" + appInfo);
            }
        }
    }

    public void commitChanges() {
        if (mChangedInfos.size() > 0) {
            Set<AppInfo> infos = mChangedInfos.keySet();
            ArrayList<AppInfo> items2Enable = new ArrayList<>();
            ArrayList<AppInfo> items2Disable = new ArrayList<>();
            for (AppInfo info : infos) {
                info.enableState = mChangedInfos.get(info);
                if (info.enableState == AppInfo.APP_ENABLE) {
                    items2Enable.add(info);
                } else {
                    items2Disable.add(info);
                }
            }
            AppInfoDaoOpe.updateAppInfos(mContext,mChangedInfos.keySet());
        }
    }

    public void cancel() {
        mChangedInfos.clear();
    }

    @Override
    public int getItemCount() {
        return mAllApps.size();
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView appIcon, appSelectedFlag;
        private TextView appName;
        private int select = 0;

        public ItemViewHolder(View itemView) {
            super(itemView);
        }

        public void verifySelectState(int select) {
            select(select);
        }

        public void select(int select) {
            this.select = select;
            if (select == 1) {
                appSelectedFlag.setVisibility(View.VISIBLE);
                appSelectedFlag.setImageResource(R.drawable.app_state_selected);
            } else {
                appSelectedFlag.setVisibility(View.GONE);
            }
        }
    }
}
