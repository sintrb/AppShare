package com.sin.android.appshare.adapter;

import android.graphics.drawable.Drawable;

public class AppItem {
	public int id;
	public String name;
	public String pkgname;
	public Drawable icon;
	public String info;
	public boolean selected = true;
	public String apkdir;
	public byte[] iconBytes;

	public AppItem(int id, String name, String pkgname, Drawable icon, String info, String apkdir) {
		super();
		this.id = id;
		this.name = name;
		this.pkgname = pkgname;
		this.icon = icon;
		this.info = info;
		this.apkdir = apkdir;
	}


}
