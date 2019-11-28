package com.liulc.sqlciphertool;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

/**
 * @author Liulc
 * @version 1.0
 * @date 2019/11/27
 */

public class SqlcipherDbUtil {
	public static final int SUCCESS = 0;
	public static final int ERROR_PARAM= 1;
	public static final int ERROR_DELETE = 2;
	public static final int ERROR_OPEN = 3;
	
	private String mDealDbPath = "";

	public String getDealDbPath() {
		return mDealDbPath;
	}

	/**
	 * 将原来未加密的数据库进行加密
	 * @param password 需要设置的密码
	 * @param decipherDbPath 未加密的数据库路径
	 * @return 加密后的数据库路径
	 * */
	public int encryptDb(Context context, final String password, final String decipherDbPath) {
		if (context==null || password==null || decipherDbPath==null || !decipherDbPath.endsWith(".db")){
			return ERROR_PARAM;
		}

		mDealDbPath = decipherDbPath.substring(0, decipherDbPath.lastIndexOf(".")) + "_encrypt.db";
		if(!deleteFile(mDealDbPath)){
			return ERROR_DELETE;
		}
		File decipherFile = context.getDatabasePath(decipherDbPath);
		if (!decipherFile.exists()){
			return ERROR_PARAM;
		}

		SQLiteDatabase decipherDb;
		try {
			decipherDb = SQLiteDatabase.openOrCreateDatabase(decipherFile, "", null);
		}catch (Exception e){
			e.printStackTrace();
			return ERROR_OPEN;
		}
		
		decipherDb.execSQL(String.format("attach database '%s' as encrypt key '%s'", mDealDbPath, password));		
		decipherDb.execSQL("begin");
		try {
			decipherDb.execSQL("select sqlcipher_export('encrypt')");
		}catch (Exception e){
			/*
			* 会出现这种错误，这个不捕捉
			* net.sqlcipher.database.SQLiteException: 
			* unknown error: Queries cannot be performed using execSQL(), use query() instead
			* */
			e.printStackTrace();
		}
		decipherDb.execSQL("commit");
		decipherDb.execSQL("detach database encrypt");
		decipherDb.close();

		
		return SUCCESS;
	}

	/**
	 * 将原来加密的数据库进行解密
	 * @param password 加密数据库的密码
	 * @param encryptDbPath 加密的数据库路径
	 * @return 解密后的数据库路径
	 * */
	public int decipherDb(Context context, final String password, final String encryptDbPath) {
		if (context==null || password==null || encryptDbPath==null || !encryptDbPath.endsWith(".db")){
			return ERROR_PARAM;
		}

		mDealDbPath = encryptDbPath.substring(0, encryptDbPath.lastIndexOf(".")) + "_decipher.db";
		if(!deleteFile(mDealDbPath)){
			return ERROR_DELETE;
		}
		File encryptFile = context.getDatabasePath(encryptDbPath);
		if (!encryptFile.exists()){
			return ERROR_PARAM;
		}

		SQLiteDatabase encryptDb;
		try {
			encryptDb = SQLiteDatabase.openOrCreateDatabase(encryptFile, password, null);
		}catch (Exception e){
			e.printStackTrace();
			return ERROR_OPEN;
		}

		encryptDb.execSQL(String.format("attach database '%s' as decipher key ''", mDealDbPath));
		encryptDb.execSQL("begin");
		try {
			encryptDb.execSQL("select sqlcipher_export('decipher')");
		}catch (Exception e){
			/*
			* 会出现这种错误，这个不捕捉
			* net.sqlcipher.database.SQLiteException: 
			* unknown error: Queries cannot be performed using execSQL(), use query() instead
			* */
			e.printStackTrace();
		}
		encryptDb.execSQL("commit");
		encryptDb.execSQL("detach database decipher");
		encryptDb.close();

		return SUCCESS;
	}
	
	private boolean deleteFile(String filePath){
		File file = new File((filePath));
		if (file.exists()){
			return file.delete();
		}
		
		return true;
	}
}
