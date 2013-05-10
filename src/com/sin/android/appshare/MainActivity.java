package com.sin.android.appshare;

import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.sin.android.appshare.adapter.AppListAdapter;
import com.sin.android.sinlibs.activities.BaseActivity;
import com.sin.android.sinlibs.base.Callable;
import com.sin.java.web.server.UrlregexMapping;
import com.sin.java.web.server.WebServer;
import com.sin.java.web.server.WebServer.Status;
import com.sin.java.web.server.util.DateUtils;

/*
 * 说明,从BaseActivity继承的Activity增加了几个方法：
 * asynCall() 是异步调用, 一般在加载数据的时候使用
 * safeCall() 是使用主线程执行, 一般在修改界面控件的时候使用
 * safeToast() 使用主线程创建Toast
 * createMessageDialog() 创建对话框
 */
public class MainActivity extends BaseActivity {
	private WebServer webServer = null;
	private EditText txtPort = null;
	private ListView lvApps = null;
	private TextView txtUrl = null;
	private AppListAdapter appListAdapter;
	private ToggleButton tbChange;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setTitle(R.string.maintitle);

		tbChange = (ToggleButton) findViewById(R.id.tb_change);
		txtPort = (EditText) findViewById(R.id.et_port);
		lvApps = (ListView) findViewById(R.id.lv_apps);
		txtUrl = (TextView) findViewById(R.id.tv_url);

		CheckBox cbSeltog = (CheckBox) findViewById(R.id.cb_selecttog);
		cbSeltog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (appListAdapter != null) {
					appListAdapter.setSeleted(isChecked);
				}
			}
		});

		txtUrl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse(txtUrl.getText().toString());
				Intent it = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(it);
			}
		});

		tbChange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					startShare();
				} else {
					stopShare();
				}
			}
		});
		this.getAppInfos();
	}

	private void getAppInfos() {
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(getResources().getString(R.string.getappinfosing));
		dialog.show();
		asynCall(new Callable() {

			@Override
			public void call(Object... args) {
				appListAdapter = new AppListAdapter(MainActivity.this);
				safeCall(new Callable() {
					@Override
					public void call(Object... args) {
						lvApps.setAdapter(appListAdapter);
						dialog.cancel();
					}
				});
			}
		});
	}

	private String intIP2String(int ip) {
		int a, b, c, d;
		long lip = ip & 0x00ffffffff;
		d = (int) ((lip >> 24) & 0x00ff);
		c = (int) ((lip >> 16) & 0x00ff);
		b = (int) ((lip >> 8) & 0x00ff);
		a = (int) ((lip >> 0) & 0x00ff);
		return String.format("%d.%d.%d.%d", a, b, c, d);
	}

	private String getWifiIp() {
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		if (ip == 0)
			return null;
		else
			return intIP2String(ip);
	}

	private void startShare() {
		if (getWifiIp() != null) {
			start();
		} else {
			DialogInterface.OnClickListener pstLsn = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					start();
				}
			};
			DialogInterface.OnClickListener ngtLsn = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					tbChange.setChecked(false);
				}
			};
			DialogInterface.OnCancelListener oclLsn = new DialogInterface.OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					tbChange.setChecked(false);
				}
			};
			Dialog dialog = crateMessageDialog(R.string.tips, R.string.wifierrtips, R.string.enyes, R.string.notyes, pstLsn, ngtLsn, oclLsn);
			dialog.show();
		}
	}

	private void stopShare() {
		stop();
	}

	private void message(int msgid) {
		this.message(getResources().getString(msgid));
	}

	private void message(String msg) {
		Dialog dialog = crateMessageDialog(getResources().getString(R.string.tips), msg, getResources().getString(R.string.iknow), null, null, null, null);
		dialog.show();
	}

	private void stop() {
		if (webServer != null) {
			webServer.stop();
			webServer = null;
		}
		txtPort.setEnabled(true);
		txtUrl.setVisibility(View.GONE);
	}

	private void start() {
		this.stopShare();

		// 需要注入的数据
		Map<String, Object> injectMap = new HashMap<String, Object>();
		injectMap.put("appItems", this.appListAdapter.getAppItems());
		injectMap.put("context", this);
		injectMap.put("startDate", DateUtils.getGTMDateTime());

		int port = Integer.parseInt(txtPort.getText().toString());
		webServer = new WebServer(port, new UrlregexMapping(), null);

		// 下载 图标 应用详细
		webServer.addHandler("/download/(.*)", "com.sin.android.appshare.handler.ShareHandler.download", injectMap);
		webServer.addHandler("/appicon/([^/]*)", "com.sin.android.appshare.handler.ShareHandler.appicon", injectMap);
		webServer.addHandler("/appinfo/([^/]*)", "com.sin.android.appshare.handler.ShareHandler.appinfo", injectMap);

		// assets 文件请求处理
		webServer.addHandler("/(.*[.html|.css|.js|.png|.ico])", "com.sin.android.appshare.handler.ShareHandler.assetFile", injectMap);
		webServer.addHandler("/.*", "com.sin.android.appshare.handler.ShareHandler.index", injectMap);
		if (webServer.start()) {
			txtPort.setEnabled(false);
			String ip = getWifiIp();
			if (ip != null) {
				String url = String.format("http://%s:%d", ip, port);
				this.txtUrl.setText(Html.fromHtml(String.format("<a href=\"%s\">%s</a>", url, url)));
				String tips = String.format(getResources().getString(R.string.sharingtips), url);
				this.message(tips);
				txtUrl.setVisibility(View.VISIBLE);
			} else {
				String url = String.format("http://%s:%d", "127.0.0.1", port);
				this.txtUrl.setText(Html.fromHtml(String.format("<a href=\"%s\">%s</a>", url, url)));
				txtUrl.setVisibility(View.VISIBLE);
				saftToast(getResources().getString(R.string.shareokbut), Toast.LENGTH_LONG);
			}
		} else {
			this.message(R.string.sharefail);
			this.safeCall(new Callable() {
				@Override
				public void call(Object... args) {
					tbChange.setChecked(false);
				}
			});
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && this.webServer != null && this.webServer.getStatus() == Status.Running) {
			// 如果返回的时候在运行的话就提示一下
			DialogInterface.OnClickListener pstLsn = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			};
			Dialog dialog = this.crateMessageDialog(R.string.tips, R.string.exittips, R.string.enyes, R.string.notyes, pstLsn, null, null);
			dialog.show();
			return false;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.stopShare();
	}
}
