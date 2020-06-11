package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        //using code from PA1, PA2A
        String filename = values.getAsString("key");
        String string = values.getAsString("value");
        Log.v("insert function", "file creation step");
        FileOutputStream outputStream;

        try {
            outputStream = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            Log.v("in content provider", "write to file, str: " + string);
            outputStream.write(string.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Log.e("e", "File write failed");
        }
        //code added ends

        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        //code added
        // using PA2A code
        /* reference taken from :https://docs.oracle.com/javase/8/docs/api/java/io/FileInputStream.html,
         * http://www.java2s.com/Code/Android/File/GetFileContentsasString.htm
         * https://www.journaldev.com/875/java-read-file-to-string
         */

        byte[] buffer = new byte[1024];
        StringBuffer sb = new StringBuffer("");
        int readLineFlag;
        FileInputStream inputStream = null;
        try {
            inputStream = getContext().openFileInput(selection);
        } catch (Exception e) {
            Log.e("E", "file open step");
            e.printStackTrace();
        }

        try {
            while((readLineFlag = inputStream.read(buffer))!=-1) {
                sb.append(new String(buffer, 0, readLineFlag));
            }
        } catch (Exception e) {
            Log.e("E", "file read error");
            e.printStackTrace();
        }


        MatrixCursor matCur = new MatrixCursor(new String[]{"key", "value"});

        Log.i("TAG", selection);
        Log.i("TAG", String.valueOf(sb));

        try {
            String[] matrixRow = {selection,String.valueOf(sb)};
            matCur.addRow(matrixRow);
        } catch(Exception e) {
            Log.e("TAG", "Error:matrixRow step");
        }
        //code added ends

        Log.v("query", selection);
        return matCur; //changed
    }
}
