package com.sin.android.appshare.adapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sin.android.appshare.R;

public class AppListAdapter extends BaseAdapter {
	private Context context = null;
	private List<AppItem> appItems = null;

	public AppListAdapter(Context context) {
		super();
		this.context = context;
		this.appItems = new ArrayList<AppItem>();

		PackageManager manager = this.context.getPackageManager();

		List<PackageInfo> appinfos = manager.getInstalledPackages(0);
		int ix = 0;
		for (int i = 0; i < appinfos.size(); i++) {
			PackageInfo appinfo = appinfos.get(i);
			if ((appinfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
				String name = appinfo.applicationInfo.loadLabel(manager).toString().trim();
				String pkgname = appinfo.packageName;
				Drawable icon = appinfo.applicationInfo.loadIcon(manager);
				String apkdir = appinfo.applicationInfo.sourceDir;
				File file = new File(apkdir);
				String info = String.format("大小:%dK 版本:%s", file.length() / 1024, appinfo.versionName);
				
				AppItem item = new AppItem(ix++, name, pkgname, icon, info, apkdir);
				
				// icon
				Drawable drawable= icon;
				Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
				drawable.draw(canvas);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
				try {
					outputStream.flush();
					item.iconBytes = outputStream.toByteArray();
				} catch (IOException e) {
					e.printStackTrace();
				}
				finally{
					try {
						outputStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				this.appItems.add(item);
			}
		}
	}

	public List<AppItem> getAppItems() {
		return appItems;
	}

	@Override
	public int getCount() {
		return appItems.size();
	}

	@Override
	public Object getItem(int position) {
		return appItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = LinearLayout.inflate(context, R.layout.item_app, null);
		AppItem appItem = (AppItem) this.getItem(position);
		ImageView icon = (ImageView) view.findViewById(R.id.iv_icon);
		TextView name = (TextView) view.findViewById(R.id.tv_name);
		TextView info = (TextView) view.findViewById(R.id.tv_info);
		
		CheckBox sel = (CheckBox) view.findViewById(R.id.cb_select);
		sel.setChecked(appItem.selected);
		sel.setTag(appItem);
		sel.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppItem itm = (AppItem)buttonView.getTag();
				itm.selected = isChecked;
			}
		});
		
		
		if (appItem.icon != null)
			icon.setImageDrawable(appItem.icon);
		if (appItem.name != null)
			name.setText(appItem.name);
		if (appItem.info != null)
			info.setText(appItem.info);
		else
			info.setVisibility(View.GONE);
		return view;
	}
	
	public void setSeleted(boolean selected){
		for(AppItem appItem: appItems){
			appItem.selected = selected;
		}
		this.notifyDataSetChanged();
	}
}
