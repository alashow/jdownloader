//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;

public class IPCheck {
    private static final String FALLBACK_CHECK_SITE = "http://service.jdownloader.org/tools/getip.php";
    private static final String FALLBACK_CHECK_REGEX = "<ip>([\\d+\\.]+?)</ip>";
    public static ArrayList<String[]> IP_CHECK_SERVICES = new ArrayList<String[]>();
    private static int IP_CHECK_INDEX = 0;
    private static final Object LOCK = new Object();

    static {
        /* setup fallback ipcheck services */
        /* Use Unittest to check this table */
        IP_CHECK_SERVICES.add(new String[] { "http://www.wieistmeineip.de/", "Ihre IP-Adresse.*?class=\"ip\">([\\d+\\.]+)</h1>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.whatismyip.com/", "<h1>Your IP Address Is: ([\\d+\\.]+)</h1>" });
        // white page IP_CHECK_SERVICES.add(new String[] {
        // "http://www.whatsmyip.org/",
        // "<title>What.*?Your IP is ([\\d+\\.]+?)</title>" });
        IP_CHECK_SERVICES.add(new String[] { "http://whatismyipaddress.com/", "<B>Your IP address is ([\\d+\\.]+)</B>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.ipaddressworld.com/", "Your computer.*?size=\\+6>([\\d+\\.]+)</FONT>" });
//        IP_CHECK_SERVICES.add(new String[] { "http://www.showmyip.com/", "IP Address properties of your Internet Connection ([\\d+\\.]+) --> " });
        IP_CHECK_SERVICES.add(new String[] { "http://www.myip.ch/", "Current IP Address: ([\\d+\\.]+)</body>" });
        IP_CHECK_SERVICES.add(new String[] { "http://ipcheckit.com/", "Your IP address is:<br/><B>([\\d+\\.]+)</B>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.findmyipaddress.info/", "My IP is.*?class=\"heading_color\">([\\d+\\.]+)<" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.meineip.de/", "<th>Das ist Deine IP-Adresse.*?>([\\d+\\.]+) </td>" });
        IP_CHECK_SERVICES.add(new String[] { "http://checkip.dyndns.org/", "Current IP Address: ([\\d+\\.]+)</body" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.anonym-surfen.com/anonym-surfen/test/", "ber Sie:</td></tr>.*?IP\\:</td><td>([\\d+\\.]+)</td>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.init7.net/support/ip-adress-test.php", "Adresse: <b>([\\d+\\.]+)</b>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.ip-adress.com/IP_adresse/", "<h2>Meine IP: ([\\d+\\.]+)</h2>" });
        IP_CHECK_SERVICES.add(new String[] { "http://myip.tsql.de/", "<b>IP\\-Adresse :</b>\\s*?([\\d+\\.]+)<" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.tracemyip.org/", "name\\s*?=\\s*?\"IP\" onclick\\s*?=\\s*?\".*?\"\\s*?value\\s*?=\\s*?\"([\\d+\\.]+)\"" });
//        IP_CHECK_SERVICES.add(new String[] { "http://www.cmyip.com/", "My IP is[^\\d]*([\\d+\\.]+)</h1>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.univie.ac.at/cgi-bin/ip.cgi", "<font color=\"#dd0000\">([\\d+\\.]+)</font>" });
        IP_CHECK_SERVICES.add(new String[] { "http://checkmyip.com/", "Your local IP address is.*?([\\d+\\.]+).*?<" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.knowmyip.com/", "Your ip is:.*?<b> ([\\d+\\.]+)</b></font>" });
        IP_CHECK_SERVICES.add(new String[] { "http://whatsmyip.net/", "Address is: <span>([\\d+\\.]+)</span>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.faqs.org/ip.php", "Your IP address is: ([\\d+\\.]+).*?</p>" });
        IP_CHECK_SERVICES.add(new String[] { "http://showip.net/", "boxmaincontent\" id=\"ipaddress\">.*?([\\d+\\.]+)<di" });
//      IP_CHECK_SERVICES.add(new String[] { "http://www.ipaddresslocation.org/", "My IP Address.*?<span class=\"pb\"><b>([\\d+\\.]+)</b></span>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.spyber.com/", "<font size=.*?><b>.*?IP: ([\\d+\\.]+)</b>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.formyip.com/", "<strong>Your IP is ([\\d+\\.]+)</strong>" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.whatismyipv6.net/", "Your IP is ([\\d+\\.]+)<" });
//        IP_CHECK_SERVICES.add(new String[] { "http://myipinfo.net/", "computer.*?IP address is:.*?<p><b>([\\d+\\.]+)</b>" });

        IP_CHECK_SERVICES.add(new String[] { "http://www.moanmyip.com/", "Your external IP address</h2>.*?<div class=.*?>([\\d+\\.]+)</div>" });
        // white page IP_CHECK_SERVICES.add(new String[] { "http://my-i-p.com/",
        // "IP Whois Data for: ([\\d+\\.]+)\"" });
        IP_CHECK_SERVICES.add(new String[] { "http://www.ip2stuff.com/", "your IP address\">.*?([\\d+\\.]+) -" });

        Collections.shuffle(IP_CHECK_SERVICES);
    }

    /**
     * Fetches the current IP Address by asking a) one of the above
     * CHECK_SERVICES (random) b) the userdefined IP check location
     * 
     * if both fails, the ip adress is looked up at the FALLBACK site.
     * 
     * WARNING: IF the user has checked "DIsable IP check", trhis function will
     * always returnt he current timestamp in ms.
     * 
     * @param br
     *            TODO
     * 
     * @return ip oder /offline
     */
    public static String getIPAddress(Browser br) {
        // if
        // (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE,
        // false)) {
        // JDLogger.getLogger().finer("IP Check is disabled. return current Milliseconds");
        // return System.currentTimeMillis() + "";
        // }
        if (br == null) {
            br = new Browser();
            br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            br.setConnectTimeout(15000);
            br.setReadTimeout(15000);
        }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {
            /* use ipcheck balancer */
            String ip = IPCheck.getBalancedIP(br);
            if (ip == null) ip = IPCheck.getBalancedIP(br);
            if (ip != null) return IPCheck.LATEST_IP = ip;
        } else {
            /* use userdefined ipcheck */
            String site = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_CHECK_SITE, IP_CHECK_SERVICES.get(0)[0]);
            String patt = SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_PATTERN, IP_CHECK_SERVICES.get(0)[1]);

            try {
                JDLogger.getLogger().finer("UserDefined IP Check via " + site);
                Pattern pattern = Pattern.compile(patt);
                Matcher matcher = pattern.matcher(br.getPage(site));
                if (matcher.find()) {
                    if (matcher.groupCount() > 0) return IPCheck.LATEST_IP = matcher.group(1);
                }
            } catch (Exception e1) {
            }
            IPCheck.LATEST_IP = null;
            JDLogger.getLogger().finer("UserDefined IP Check failed. IP not found via regex: " + patt + " on " + site);
        }
        /* fallback ipcheck */
        try {
            JDLogger.getLogger().finer("Fallback IP Check via JDownloader-IPCheck");
            Pattern pattern = Pattern.compile(FALLBACK_CHECK_REGEX);
            Matcher matcher = pattern.matcher(br.getPage(FALLBACK_CHECK_SITE));
            if (matcher.find()) {
                if (matcher.groupCount() > 0) return IPCheck.LATEST_IP = matcher.group(1);
            }
        } catch (Exception e1) {
        }
        IPCheck.LATEST_IP = null;
        JDLogger.getLogger().finer("Fallback IP Check failed.");

        return "offline";
    }

    /**
     * Returns the latest ip. Does only invoke a real mlookup if there is no
     * cached IP
     * 
     * @return
     */
    public static String getLatestIP() {
        if (IPCheck.LATEST_IP == null) getIPAddress(null);
        return IPCheck.LATEST_IP;
    }

    /**
     * USes IP_CHECK_SERVICES to get the current IP. rotates through
     * IP_CHECK_SERVICES which is random sorted.
     * 
     * @param br
     * @return current IP string or null if check fails
     */
    public static String getBalancedIP(Browser br) {
        if (IP_CHECK_SERVICES.size() == 0) return null;
        if (br == null) {
            br = new Browser();
            br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            br.setConnectTimeout(10000);
            br.setReadTimeout(10000);
        }
        synchronized (LOCK) {
            IP_CHECK_INDEX = IP_CHECK_INDEX % IP_CHECK_SERVICES.size();
            String[] ipcheck = IP_CHECK_SERVICES.get(IP_CHECK_INDEX);
            IP_CHECK_INDEX++;
            if (ipcheck.length != 2) return null;
            try {
                Pattern pattern = Pattern.compile(ipcheck[1], Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(br.getPage(ipcheck[0]));
                if (matcher.find()) {
                    if (matcher.groupCount() > 0) return LATEST_IP = matcher.group(1);
                }
            } catch (Exception e) {
            }
            JDLogger.getLogger().finer("Balance IP Check failed. IP not found via regex: " + ipcheck[1] + " on " + ipcheck[0]);
        }
        return null;
    }

    public static String LATEST_IP = null;

}
