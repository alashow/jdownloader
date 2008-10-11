//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.jdunrar;

public class JDUnrarConstants {

    public static final int NO_START_PART = 0;
    public static final int MULTIPART_START_PART = 1;
    public static final int SINGLE_PART_ARCHIVE = 2;
    public static final int NO_RAR_ARCHIVE = 3;
    public static final String CONFIG_KEY_WAITLIST = "WAIT_LIST";
    public static final String CONFIG_KEY_LIST = "LIST";
    public static final String CONFIG_KEY_UNRARCOMMAND = "UNRARCOMMAND";
    public static final int WRAPPER_PROGRESS_SINGLE_FILE_FINISHED = 4;
    public static final int WRAPPER_PROGRESS_NEW_SINGLE_FILE_STARTED = 5;
    public static final int WRAPPER_ON_PROGRESS = 6;
    public static final int WRAPPER_STARTED = 7;
    public static final int WRAPPER_START_OPEN_ARCHIVE = 8;
    public static final int WRAPPER_OPEN_ARCHIVE_SUCCESS = 9;
    public static final int WRAPPER_PASSWORD_FOUND = 10;
    public static final int WRAPPER_NEW_STATUS = 11;
    public static final int WRAPPER_EXTRACTION_FAILED = 12;
    public static final int WRAPPER_START_EXTRACTION = 13;
    public static final int WRAPPER_FAILED_PASSWORD = 14;
    public static final int WRAPPER_FINISHED_SUCCESSFULL = 15;
    public static final int WRAPPER_EXTRACTION_FAILED_CRC = 16;
    public static final int WRAPPER_CRACK_PASSWORD = 17;
    public static final String CONFIG_KEY_UNRARPATH = "UNRARPATH";

}
