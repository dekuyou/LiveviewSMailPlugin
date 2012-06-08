package jp.ddo.dekuyou.liveview.plugins.smail;

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.SmsMessage;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;

public class SmailPluginService extends AbstractPluginService {

	// Our handler.
	private Handler mHandler = null;

	private String phoneNo = "";
	private String subject = "";
	private String body = "";
	private String date = "";

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.initialize(this);

		// Create handler.
		if (mHandler == null) {
			mHandler = new Handler();
		}

		Log.d(this.getClass().getPackage().getName(),
				"SmailPluginService onStart");

		if(intent == null){
			Log.d("intent is null.");
			return;
		}
		
		Bundle extras = intent.getExtras();
		if (extras != null) {
			
			phoneNo = "";
			subject = "";
			body = "";
			date = "";

			try {
				if (extras.get("pdus") != null) {
					// SMS
					// pduのデコードとログ出力
					// サンプルのためBroadcastReceiverで処理(本来はServiceで)
					Object[] pdus = (Object[]) extras.get("pdus");
					for (Object pdu : pdus) {
						SmsMessage smsMessage = SmsMessage
								.createFromPdu((byte[]) pdu);
						Log.d(this.getClass().getPackage().getName(), "from:"
								+ smsMessage.getOriginatingAddress());
						Log.d(this.getClass().getPackage().getName(),
								"time:"
										+ Long.toString(smsMessage
												.getTimestampMillis()));
						Log.d(this.getClass().getPackage().getName(),
								"body:"
										+ smsMessage.getMessageBody()
												.replaceAll("\n", "\t"));

						phoneNo = smsMessage.getOriginatingAddress();
						body = body + smsMessage.getMessageBody();
						date = Long.toString(smsMessage.getTimestampMillis());

					}
				} else if (extras.get("deleted_contents") != null) {

					// MMS
					String _id = extras.get("deleted_contents").toString();

					if (_id.startsWith("content://mms/inbox/")) {

						Cursor curPdu = getContentResolver().query(
								Uri.parse("content://mms"), null, null, null,
								null);

						curPdu.moveToFirst();

						String id = curPdu.getString(curPdu
								.getColumnIndex("_id"));
						Log.d(this.getPackageName(), id);

						String thread_id = curPdu.getString(curPdu
								.getColumnIndex("thread_id"));
						Log.d(this.getPackageName(), thread_id);

						// subject =
						// MimeUtility.decodeText(curPdu.getString(curPdu.getColumnIndex("sub")));

						String tmpsbj = curPdu.getString(curPdu
								.getColumnIndex("sub")) != null ? curPdu
								.getString(curPdu.getColumnIndex("sub")) : "";

						subject = new String(tmpsbj.getBytes("ISO8859_1"),
								"utf-8");

						Log.d(this.getPackageName(), String.valueOf(subject));

						date = String.valueOf(curPdu.getLong(curPdu
								.getColumnIndex("date")) * 1000L);
						Log.d(this.getPackageName(), String.valueOf(date));

						_id = String
								.valueOf((new Integer(_id.substring(20)) + 1));
						Log.d(this.getPackageName(), String.valueOf(_id));

						Uri uriAddr = Uri.parse("content://mms/" + _id
								+ "/addr");
						Cursor curAddr = getContentResolver().query(uriAddr,
								null, null, null, null);
						if (curAddr.moveToNext()) {

							phoneNo = curAddr.getString(curAddr
									.getColumnIndex("address"));
							Log.d(this.getPackageName(), String.valueOf(phoneNo));

							Cursor curPart = getContentResolver()
									.query(Uri.parse("content://mms/" + _id
											+ "/part"), null, null, null, null);
							String[] coloumns = null;
							String[] values = null;

							while (curPart.moveToNext()) {
								coloumns = curPart.getColumnNames();
								if (values == null)
									values = new String[coloumns.length];

								for (int i = 0; i < curPart.getColumnCount(); i++) {
									values[i] = curPart.getString(i);
									Log.d(this.getPackageName(), i + ":"
											+ String.valueOf(values[i]));

								}
								String contact_idd = curPart.getString(0);
								Log.d(this.getPackageName(),
										String.valueOf(contact_idd));

								if (values[3].equals("text/plain")) {

									body = values[13].replaceAll("\n", "\t");

								}
							}

							// date = String.valueOf(Calendar.getInstance()
							// .getTimeInMillis());

						}

						if (!"".equals(subject)) {
							body = "<"+ getString(R.string.subject) +":" + subject + ">\n" + body;

						}

					} else if (_id.startsWith("content://mms/")) {
						Log.d("content://mms/");
						//
						// phonNo = "?";
						// body = "未取得のメッセージ";
						// date = String.valueOf(Calendar.getInstance()
						// .getTimeInMillis());

					} else {
						Log.d("MMS not content://mms/inbox/");
						// this.stopSelf();
						return;
					}

				}
			} catch (Exception e) {
				// TODO: handle exception
				Log.d(e);
				// this.stopSelf();
				return;
			}

			Log.d("phonNo:" + phoneNo);
			Log.d("body:" + body);
			Log.d("date:" + date);
			
			phoneNo = getName(phoneNo);
			
			Log.d("phonNo_:" + phoneNo);

			sendAnnounce(phoneNo, body);
		}

	}
	
	private String getName(String phoneNo) {
		if(phoneNo.indexOf("@") > 0){
			return phoneNo;
		}
		
		String ret = phoneNo;

		String[] proj = new String[] { Phone._ID, Phone.DISPLAY_NAME,
				Phone.NUMBER };

		Uri _uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri
				.encode(phoneNo));

		Cursor _cursor = getContentResolver().query(_uri, proj, null, null,
				null);

		if (_cursor.getCount() > 0) {
			_cursor.moveToFirst();

			ret = _cursor.getString(1);
		}

		_cursor.close();

		return ret;

	}

	@Override
	public void onCreate() {
		super.onCreate();



		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		stopWork();
	}

	boolean sandbox = false;

	/**
	 * Plugin is sandbox.
	 */
	protected boolean isSandboxPlugin() {
		return sandbox;
	}

	private void sendAnnounce(String header, String body) {

		try {
			Log.d(mLiveViewAdapter.toString());
			Log.d(String.valueOf(mPluginId));
			Log.d(mMenuIcon.toString());

			mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, header, body,
					System.currentTimeMillis(),
					"");
			Log.d(this.getPackageName(), "Announce sent to LiveView");

		} catch (Exception e) {
			Log.e(this.getPackageName(), "Failed to send announce", e);
		}
	}

	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

	}

	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView
	 * Service.
	 * 
	 * If needed, do additional actions here, e.g. starting any worker that is
	 * needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className,
			IBinder service) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been
	 * stopped.
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {

	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed.
	 */
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs,
			String key) {

	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");

		sandbox = false;
		startWork();
	}

	protected void stopPlugin() {
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");

		sandbox = false;

		stopWork();
	}

	protected void button(String buttonType, boolean doublepress,
			boolean longpress) {
		Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType
				+ ", doublepress " + doublepress + ", longpress " + longpress);

		if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {

		} else if (buttonType.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {

		}

	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
		Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx
				+ ", height " + displayHeigthPx);
	}

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		stopWork();
	}

	protected void openInPhone(String openInPhoneAction) {
		Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);
		
		
		// jp.softbank.mb.mail
		
		// 
		// com.sonyericsson.conversations
		// jp.co.sharp.android.messaging 
		
		
		// com.android.mms
		
		// com.jb.mms
		
		
		//
		
        PackageManager pm = this.getPackageManager();
 
    
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        
    
        
        //
        List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        
        Log.d(list.toString());
        
        String pkgNmae = "";
        String clazz = "";
        
        Log.d("ResolveInfo.size():"+list.size());
 
        //パッケージ情報をリストビューに追記
        for (ResolveInfo ai : list) {
            
            Log.d("appname"+ ai.loadLabel(pm).toString());
            Log.d("packageName"+ ai.activityInfo.applicationInfo.packageName);
            Log.d("className"+ ai.activityInfo.name);
            
            if(ai.activityInfo.applicationInfo.packageName.indexOf("jp.softbank.mb.mail") > -1){
        		// jp.softbank.mb.mail
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            	
            }else if(ai.activityInfo.applicationInfo.packageName.indexOf("com.sonyericsson.conversations") > -1){
            	// com.sonyericsson.conversations
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            	
            }else if (ai.activityInfo.applicationInfo.packageName.indexOf("com.android.mms") > -1){
            	// com.android.mms
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            }else if (ai.activityInfo.applicationInfo.packageName.indexOf("com.jb.mms") > -1){
            	// com.jb.mms	
            	pkgNmae =  ai.activityInfo.applicationInfo.packageName;
            	clazz = ai.activityInfo.name;
            	break;
            }
            
            
            

        }
		

        Log.d("_appname:"+ clazz);
        Log.d("_packageName:"+ pkgNmae);

        try {
			// Open in .
			final Intent browserIntent = new Intent(Intent.ACTION_MAIN);
			browserIntent.setClassName(pkgNmae,clazz);
			browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(browserIntent);
		} catch (Exception e) {
			Log.e(e);
		}
			
	}

	protected void screenMode(int mode) {
		Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now "
				+ ((mode == 0) ? "OFF" : "ON"));

		if (mode == PluginConstants.LIVE_SCREEN_MODE_ON) {
		} else {
		}
	}

}