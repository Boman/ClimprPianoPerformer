/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package org.climpr.pianoperformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import org.climpr.pianoperformer.midi.MidiFileException;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.net.Uri;


/** @class FileUri
 * Represents a reference to a file.
 * The file could be either in the /assets directory, 
 * the internal storage, or the external storage.
 */
public class FileUri implements Comparator<FileUri> {
    private AssetManager asset;       /** For reading files in /assets */
    private String filepath;          /** The path to the file */

    private ContentResolver resolver; /** For reading from storage */
    private Uri uri;                  /** The URI path to the file */

    private String displayName;       /** The name to display */

    /** Create a new reference to a file under /assets */
    public FileUri(AssetManager asset, String path, String display) {
        this.asset = asset;
        filepath = path;
        displayName = display;
        displayName = displayName.replace("__", ": ");
        displayName = displayName.replace("_", " ");
        displayName = displayName.replace(".mid", "");
    }

    /** Create a new reference to a file in internal/external storage.
     *  The URI should be MediaStore.Audio.Media.EXTERNAL_CONTENT_URI/id
     */
    public FileUri(ContentResolver resolver, Uri uri, String display) {
        this.resolver = resolver;
        this.uri = uri;
        displayName = display;
        displayName = displayName.replace("__", ": ");
        displayName = displayName.replace("_", " ");
        displayName = displayName.replace(".mid", "");
    }

    /** Return the display name */
    public String toString() {
        return displayName;
    }

    /** Compare two files by their display name */
    public int compare(FileUri f1, FileUri f2) {
        return f1.displayName.compareToIgnoreCase(f2.displayName);
    }

    /** Return the file contents as a byte array.
     *  If any IO error occurs, return null.
     */
    public byte[] getData() {
        try {
            byte[] data;
            int totallen, len, offset;
        
            // First, determine the file length
            data = new byte[4096];
            InputStream file;
            if (asset != null) {
                file = asset.open(filepath);
            }
            else {
                // Uri uri = Uri.withAppendedPath(
                //        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + uriID);
                file = resolver.openInputStream(uri);
            }
            totallen = 0;
            len = file.read(data, 0, 4096);
            while (len > 0) {
                totallen += len;
                len = file.read(data, 0, 4096);
            }
            file.close();
        
            // Now read in the data
            offset = 0;
            data = new byte[totallen];

            if (asset != null) {
                file = asset.open(filepath);
            }
            else {
                // Uri uri = Uri.withAppendedPath(
                //        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + uriID);
                file = resolver.openInputStream(uri);
            }
            while (offset < totallen) {
                len = file.read(data, offset, totallen - offset);
                if (len <= 0) {
                    throw new MidiFileException("Error reading midi file", offset);
                }
                offset += len;
            }
            return data;
        }
        catch (IOException e) {
            return null;
        }
    }
}

