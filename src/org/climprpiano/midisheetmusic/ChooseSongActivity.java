/*
 * Copyright (c) 2011-2013 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package org.climprpiano.midisheetmusic;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.util.Log;
import android.content.*;

import org.climprpiano.PianoActivity;
import org.climprpiano.R;
import org.json.*;

import android.graphics.*;
import android.graphics.drawable.*;

/** @class ChooseSongActivity
 * The ChooseSongActivity class is a tabbed view for choosing a song to play.
 * There are 3 tabs:
 * - All    (AllSongsActivity)    : Display a list of all songs
 * - Recent (RecentSongsActivity) : Display of list of recently opened songs
 * - Browse (FileBrowserActivity) : Let the user browse the filesystem for songs
 */
public class ChooseSongActivity extends TabActivity {

    static ChooseSongActivity globalActivity;

    @Override
    public void onCreate(Bundle state) {
        globalActivity = this;
        super.onCreate(state);

       
        Bitmap allFilesIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.allfilesicon);
        Bitmap recentFilesIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.recentfilesicon);
        Bitmap browseFilesIcon = BitmapFactory.decodeResource(this.getResources(), R.drawable.browsefilesicon);

        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("All")
                .setIndicator("All", new BitmapDrawable(allFilesIcon))
                .setContent(new Intent(this, AllSongsActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("Recent")
                .setIndicator("Recent", new BitmapDrawable(recentFilesIcon))
                .setContent(new Intent(this, RecentSongsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        tabHost.addTab(tabHost.newTabSpec("Browse")
                .setIndicator("Browse", new BitmapDrawable(browseFilesIcon))
                .setContent(new Intent(this, FileBrowserActivity.class)));

        tabHost.setCurrentTab(1);
    }

    public static void openFile(FileUri file) {
        globalActivity.doOpenFile(file);
    }

    public void doOpenFile(FileUri file) {
        byte[] data = file.getData(this);
        if (data == null || data.length <= 6) { //TODO || !MidiFile.hasMidiHeader(data)) {
            ChooseSongActivity.showErrorDialog("Error: Unable to open song: " + file.toString(), this);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, file.getUri(), this, PianoActivity.class);
        intent.putExtra(PianoActivity.MidiTitleID, file.toString());
        startActivity(intent);
    }


    /** Show an error dialog with the given message */
    public static void showErrorDialog(String message, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
           }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
}

