package jp.ddo.dekuyou.liveview.plugins.smail;

import java.util.Set;

import jp.ddo.dekuyou.liveview.plugins.smail.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String WAPPUSH_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
	private static final String MMS_RECEIVED = "android.intent.action.CONTENT_CHANGED";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.initialize(context);

		Log.d("SMSReceiver onReceive : " + intent.getAction());

		if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {

			Log.d(this.getClass().getPackage().getName(), "NEW_OUTGOING_CALL");

		} else if (Intent.ACTION_PROVIDER_CHANGED.equals(intent.getAction())) {

			Log.d(this.getClass().getPackage().getName(), "gmail received");

		} else if (SMS_RECEIVED.equals(intent.getAction())) {

			Log.d(this.getClass().getPackage().getName(), "sms received");

			Bundle extras = intent.getExtras();
			if (extras != null) {
				// pduのデコードとログ出力
				//
				Set<String> set = extras.keySet();
				for (String str : set) {
					Log.d(this.getClass().getPackage().getName(), "extras:"
							+ str + ":" + extras.get(str).toString());

				}

				Object[] pdus = (Object[]) extras.get("pdus");
				for (Object pdu : pdus) {
					SmsMessage smsMessage = SmsMessage
							.createFromPdu((byte[]) pdu);
					Log.d(this.getClass().getPackage().getName(), "from:"
							+ smsMessage.getOriginatingAddress());
					Log.d(this.getClass().getPackage().getName(), "time:"
							+ Long.toString(smsMessage.getTimestampMillis()));
					Log.d(this.getClass().getPackage().getName(), "body:"
							+ smsMessage.getMessageBody()
									.replaceAll("\n", "\t"));
				}
			}

			// サービス起動
			intent = new Intent(context, SmailPluginService.class);
			intent.putExtras(extras);
			context.startService(intent);

		} else if (WAPPUSH_RECEIVED.equals(intent.getAction())) {

			Log.d(this.getClass().getPackage().getName(), "wap push received");

			Bundle extras = intent.getExtras();
			if (extras != null) {
				int transactionId = extras.getInt("transactionId");
				int pduType = extras.getInt("pduType");
				byte[] header = extras.getByteArray("header");
				byte[] data = extras.getByteArray("data");

				Log.d("contentType: "
						+ ((intent.getType() != null) ? intent.getType() : ""));
				Log.d("transactionId: " + Integer.toString(transactionId));
				Log.d("pduType: " + Integer.toString(pduType));

				String headerStr = "";
				String dataStr = "";

				if (header != null) {
					for (int i = 0; i < header.length; i++) {
						Log.d(String.format("header[%03d]: 0x%02x (%s)", i,
								header[i], (char) header[i]));
					}
				} else {
					Log.d("header is null");
				}

				if (data != null) {
					for (int i = 0; i < data.length; i++) {
						Log.d(String.format("data[%03d]: 0x%02x (%s)", i,
								data[i], (char) data[i]));
					}
				} else {
					Log.d("data is null");
				}

				Intent it = new Intent();
				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				it.setAction(Intent.ACTION_SENDTO);
				it.setData(Uri.parse("mailto:" + "android.dev@ucbsweb.ddo.jp"));
				it.putExtra(Intent.EXTRA_SUBJECT, this.getClass().getPackage()
						.getName());
				it.putExtra(Intent.EXTRA_TEXT, intent.getAction()
						+ "\n-----------\n" + headerStr + "\n-----------\n"
						+ dataStr);
				context.startActivity(it);

			}

		} else if (MMS_RECEIVED.equals(intent.getAction())) {

			Log.d(this.getClass().getPackage().getName(), "mms received");

			Bundle extras = intent.getExtras();
			if (extras != null) {

				Set<String> set = extras.keySet();
				for (String str : set) {
					Log.d(this.getClass().getPackage().getName(), "extras:"
							+ str + ":" + extras.get(str).toString());

				}

				// サービス起動
				intent = new Intent(context, SmailPluginService.class);
				intent.putExtras(extras);
				context.startService(intent);

			}

		} else {
//			Log.d("docomo?" + intent.getAction());
//
//			Bundle extras = intent.getExtras();
//			if (extras != null) {
//
//				String extrasStr = "";
//				Set<String> set = extras.keySet();
//				for (String str : set) {
//					Log.d(this.getClass().getPackage().getName(), "extras:"
//							+ str + ":" + extras.get(str).toString());
//					extrasStr = "extras:" + str + ":"
//							+ extras.get(str).toString() + "\n";
//
//				}
//
//				Intent it = new Intent();
//				it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				it.setAction(Intent.ACTION_SENDTO);
//				it.setData(Uri.parse("mailto:" + "android.dev@ucbsweb.ddo.jp"));
//				it.putExtra(Intent.EXTRA_SUBJECT, this.getClass().getPackage()
//						.getName());
//				it.putExtra(Intent.EXTRA_TEXT, intent.getAction()
//						+ "\n-----------\n" + extrasStr);
//				context.startActivity(it);
//
//			}

		}

	}
}
