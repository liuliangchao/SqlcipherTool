package com.liulc.sqlciphertool;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;


public class MainActivity extends AppCompatActivity implements FileChooserDialog.FileCallback, View.OnClickListener{	
	private TextView tv_msg;
	private MaterialDialog mWaitDialog;
	private  SqlcipherDbUtil dbUtil = new SqlcipherDbUtil();
	private long startTime = System.currentTimeMillis();

	private String lastPath = Environment.getExternalStorageDirectory().getPath();
	private String dealDbPath = "";
	private String password = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SQLiteDatabase.loadLibs(this);

		tv_msg = (TextView) findViewById(R.id.tv_msg); 
		bindButton(R.id.bt_chooseDb);
		bindButton(R.id.bt_password);
		bindButton(R.id.bt_encryptDb);
		bindButton(R.id.bt_decipherDb);

		tv_msg.setMovementMethod(ScrollingMovementMethod.getInstance());

		PermissionGen.needPermission(this, 100,
				new String[] {
						Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE
				}
		);
	}

	@PermissionFail(requestCode = 100)
	public void doFailSomething(){
		Toast.makeText(this, getString(R.string.permission_fail), Toast.LENGTH_SHORT).show();
	}

	@Override 
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
	}
	
	private Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message message) {
			switch (message.what){
				case SqlcipherDbUtil.ERROR_PARAM:
					setTextMsg(getString(R.string.error_param));
					mWaitDialog.dismiss();
					break;
				case SqlcipherDbUtil.ERROR_DELETE:
					setTextMsg(getString(R.string.error_delete));
					mWaitDialog.dismiss();
					break;
				case SqlcipherDbUtil.ERROR_OPEN:
					setTextMsg(getString(R.string.error_open));
					mWaitDialog.dismiss();
					break;
				case SqlcipherDbUtil.SUCCESS:
					setTextMsg(String.format(getString(R.string.deal_finish), dbUtil.getDealDbPath()));
					setTextMsg(String.format(getString(R.string.used_time), (System.currentTimeMillis()-startTime)));
					mWaitDialog.dismiss();
					break;
			}
			return false;
		}
	});
	
	private void setTextMsg(String msg) {
		String content = tv_msg.getText() + "\n" + msg;
		tv_msg.setText(content);
	}
	
	private Button bindButton(int id) {
		Button bt = (Button) findViewById(id);
		bt.setOnClickListener(this);
		
		return bt;
	}
	
	@Override
	public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
		dealDbPath = file.getAbsolutePath();
		lastPath = file.getParentFile().getAbsolutePath();
		String content = "选择数据库: " + dealDbPath;
		tv_msg.setText(getString(R.string.about));
		setTextMsg(content);
	}
	
	@Override
	public void onFileChooserDismissed(FileChooserDialog dialog){
	}
	
	@Override
	public void onClick(View v){
		switch (v.getId()){
			case R.id.bt_chooseDb:
				new FileChooserDialog.Builder(this)
					.initialPath(lastPath)
					.mimeType("*/*")
					.tag("optional-identifier")
					.show(this);
				break;
			case R.id.bt_password:
				new MaterialDialog.Builder(this)
						.title(R.string.password_title)
						.content(R.string.password_content)
						.inputType(InputType.TYPE_CLASS_TEXT)
						.input(null, password, new MaterialDialog.InputCallback() {
							@Override
							public void onInput(MaterialDialog dialog, CharSequence input) {
								password = input.toString();
							}
						}).show();
				break;
			case R.id.bt_encryptDb:
				showWaitDialog();
				new Thread(new Thread(){
					@Override
					public void run(){
						startTime = System.currentTimeMillis();
						int ret = dbUtil.encryptDb(MainActivity.this, password, dealDbPath);
						mHandler.sendEmptyMessage(ret);
					}
				}).start();
				break;
			case R.id.bt_decipherDb:
				showWaitDialog();
				new Thread(new Thread(){
					@Override
					public void run(){
						startTime = System.currentTimeMillis();
						int ret = dbUtil.decipherDb(MainActivity.this, password, dealDbPath);
						mHandler.sendEmptyMessage(ret);
					}
				}).start();
				break;
		}
	}
	
	private void showWaitDialog() {
		MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
				.title(R.string.wait_title)
				.content(R.string.wait_content)
				.progress(true, 0)
				.autoDismiss(true)
				.cancelable(false)
				.progressIndeterminateStyle(true);
		
		mWaitDialog = builder.build();
		mWaitDialog.show();
	}
}
