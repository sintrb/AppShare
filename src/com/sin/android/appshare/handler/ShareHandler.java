package com.sin.android.appshare.handler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

import android.content.Context;

import com.sin.android.appshare.MainActivity;
import com.sin.android.appshare.R;
import com.sin.android.appshare.adapter.AppItem;
import com.sin.java.web.server.BaseHandler;
import com.sin.java.web.server.util.DateUtils;

public class ShareHandler extends BaseHandler {
	public Context context;
	public List<AppItem> appItems;
	public Date startDate;
	private static boolean usingCache = false;

	public boolean needNew() {
		if (usingCache == false)
			return true;
		responseHeader.set("Last-Modified", DateUtils.toGMTString());
		Date sinceDate = DateUtils.fromGTMString(requestHeader.get("If-Modified-Since"));
		return sinceDate == null || (sinceDate.getTime() / 1000) < (startDate.getTime() / 1000 - 10);
	}

	public String index() {
		if (needNew() == false)
			respose304();
		String res = null;
		StringBuffer sb = new StringBuffer();
		String itemTpl = stringFromAssets("tpappitem.html");
		for (AppItem appItem : appItems) {
			if (appItem.selected == false)
				continue;
			String item = itemTpl.replace("$id", "" + appItem.id).replace("$name", appItem.name);
			sb.append(item);
			sb.append("\n");
		}
		String tp = stringFromAssets("tpindex.html");
		if (tp != null) {
			String cprt = String.format("Copyright © 2009 - %d Sin Corporation, All Rights Reserved", startDate.getYear() + 1900);
			res = tp.replace("$title", "App Share").replace("$copyright", cprt).replace("$list", sb.toString());
		} else {
			res = sb.toString();
		}
		return res;
	}

	public String appicon(String id) {
		if (needNew() == false)
			respose304();
		String res = null;
		try {
			AppItem appItem = this.appItems.get(Integer.parseInt(id));
			if (appItem.iconBytes != null) {
				ByteArrayInputStream iconStream = new ByteArrayInputStream(appItem.iconBytes);
				responseStream(iconStream, "image/png");
				iconStream.close();
				webServer.log("get appicon %s successed!", id);
			} else {
				responseHeader.setCode(404);
				responseHeader.setDescribe("Not Found");
				res = "Not Found icon: " + id;
				webServer.log(res);
			}
		} catch (Exception e) {
			webServer.log("get appicon %s failed!", id);
			webServer.err(e);
		}
		return res;
	}

	public String download(String id) {
		if (needNew() == false)
			respose304();
		try {
			AppItem appItem = this.appItems.get(Integer.parseInt(id));
			File file = new File(appItem.apkdir);
			FileInputStream inputStream = new FileInputStream(file);
			this.responseHeader.set("Content-Disposition", "attachment; filename=\"" + appItem.pkgname + ".apk\"");
			
			// response apk
			responseStream(inputStream, -1, file.length(), "application/vnd.android.package-archive");
			inputStream.close();
			webServer.log("download pkg %s successed!", id);
			String msg = String.format(context.getResources().getString(R.string.downedmsg), appItem.name);
			((MainActivity) context).saftToast(msg);
		} catch (Exception e) {
			webServer.log("download pkg %s failed!", id);
			webServer.err(e);
		}
		return null;
	}

	public String appinfo(String id) {
		if (needNew() == false)
			respose304();
		String res = null;
		try {
			AppItem appItem = this.appItems.get(Integer.parseInt(id));
			String tpl = stringFromAssets("tpappinfo.html");
			if (tpl != null) {
				res = tpl.replace("$id", "" + appItem.id).replace("$name", appItem.name).replace("$info", appItem.info);
			}
		} catch (Exception e) {
			webServer.err(e);
		}
		return res;
	}

	private String stringFromAssets(String filename) {
		String res = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
			StringBuffer sb = new StringBuffer();
			String l;
			while ((l = br.readLine()) != null) {
				sb.append(l);
				sb.append('\n');
			}
			res = sb.toString();
			br.close();
		} catch (IOException e) {
			res = null;
			webServer.err(e);
		}
		return res;
	}

	public String assetFile(String filename) {
		if (needNew() == false)
			respose304();
		String res = "get: " + filename;
		if (filename.endsWith(".js")) {
			// js
			this.setContentType("application/x-javascript");
		} else if (filename.endsWith(".css")) {
			// css
			this.setContentType("text/css");
		} else if (filename.endsWith(".html")) {
			// html
			this.setContentType("text/html; charset=utf-8");
		} else if (filename.endsWith(".png")) {
			// png
			this.setContentType("image/png");
		} else if (filename.endsWith(".ico")) {
			// png
			this.setContentType("image/x-icon");
		}
		try {
			InputStream inputStream = context.getAssets().open(filename);
			this.responseStream(inputStream, null);
			inputStream.close();
		} catch (IOException e) {
			webServer.err(e);
			responseHeader.setCode(404);
			responseHeader.setDescribe("Not Found");
			res = e.getMessage();
			webServer.log("Not Found: " + filename);
		}
		return res;
	}

	public String respose304() {
		responseHeader.setCode(304);
		responseHeader.set("Last-Modified", DateUtils.toGMTString(startDate));
		return null;
	}

}
