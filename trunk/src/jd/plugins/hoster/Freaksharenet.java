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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakshare.net" }, urls = { "http://[\\w\\.]*?freakshare\\.net/file(s/|/)[\\w]+/(.*)" }, flags = { 2 })
public class Freaksharenet extends PluginForHost {

    public Freaksharenet(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
        this.enablePremium("http://freakshare.net/shop.html");
    }

    // @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");/* workaround for buggy server */
        br.setFollowRedirects(false);
        br.getPage("http://freakshare.net/?language=US"); /*
                                                           * set english
                                                           * language in
                                                           * phpsession
                                                           */
        br.getPage("http://freakshare.net/login.html");
        br.postPage("http://freakshare.net/login.html", "user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&submit=Login");
        if (br.getCookie("http://freakshare.net", "login") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://freakshare.net/");
        if (!br.containsHTML("<td><b>Member \\(premium\\)</b></td>")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String left = br.getRegex(">Traffic left:</td>.*?<td>(.*?)</td>").getMatch(0);
        ai.setTrafficLeft(left);
        String validUntil = br.getRegex(">valid until:</td>.*?<td><b>(.*?)</b></td>").getMatch(0);
        if (validUntil == null) {
            account.setValid(false);
        } else {
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "dd.MM.yyyy - HH:mm", null));
            account.setValid(true);
        }
        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String url = null;
        if (br.getRedirectLocation() == null) {
            Form form = br.getForm(0);
            br.submitForm(form);
            url = br.getRedirectLocation();
        } else {
            url = br.getRedirectLocation();
        }
        if (url == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, url, true, 0);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("http://freakshare.net/?language=US"); /*
                                                           * set english
                                                           * language in
                                                           * phpsession
                                                           */
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("We are back soon")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        if (br.containsHTML("Sorry but this File is not avaible")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1[^>]*>(.*?)</h1>").getMatch(0).trim();
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * /* public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("You can Download only 1 File in")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001);
        Form form = br.getForm(1);
        sleep(50 * 1000l, downloadLink);
        br.submitForm(form);
        form = br.getForm(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, form, false, 1);

        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("you cant  download more then 1 at time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1001);
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {

    }
}
