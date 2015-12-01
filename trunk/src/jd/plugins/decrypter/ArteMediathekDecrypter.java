//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.TimeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "arte.tv", "concert.arte.tv", "creative.arte.tv", "future.arte.tv", "cinema.arte.tv" }, urls = { "http://www\\.arte\\.tv/guide/(?:de|fr)/\\d+\\-\\d+(?:\\-[ADF])?/[a-z0-9\\-_]+", "http://concert\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+", "http://creative\\.arte\\.tv/(?:de|fr)/(?!scald_dmcloud_json)[a-z0-9\\-]+(/[a-z0-9\\-]+)?", "http://future\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+(/[a-z0-9\\-]+)?", "http://cinema\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+(/[a-z0-9\\-]+)?" }, flags = { 0, 0, 0, 0, 0 })
public class ArteMediathekDecrypter extends PluginForDecrypt {

    private static final String     EXCEPTION_LINKOFFLINE                       = "EXCEPTION_LINKOFFLINE";

    private static final String     TYPE_CONCERT                                = "http://(www\\.)?concert\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+";
    private static final String     TYPE_CREATIVE                               = "http://(www\\.)?creative\\.arte\\.tv/(?:de|fr)/.+";
    private static final String     TYPE_FUTURE                                 = "http://(www\\.)?future\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+(/[a-z0-9\\-]+)?";
    private static final String     TYPE_GUIDE                                  = "http://www\\.arte\\.tv/guide/(?:de|fr)/\\d+\\-\\d+(?:\\-[ADF])?/[a-z0-9\\-_]+";
    private static final String     TYPE_CINEMA                                 = "http://cinema\\.arte\\.tv/(?:de|fr)/[a-z0-9\\-]+(/[a-z0-9\\-]+)?";

    private static final String     API_TYPE_GUIDE                              = "^http://(www\\.)?arte\\.tv/papi/tvguide/videos/stream/player/[ADF]/.+\\.json$";
    private static final String     API_TYPE_CINEMA                             = "^https?://api\\.arte\\.tv/api/player/v1/config/(?:de|fr)/([A-Za-z0-9\\-]+)\\?vector=.+";

    private static final String     V_NORMAL                                    = "V_NORMAL";
    private static final String     V_SUBTITLED                                 = "V_SUBTITLED";
    private static final String     V_SUBTITLE_DISABLED_PEOPLE                  = "V_SUBTITLE_DISABLED_PEOPLE";
    private static final String     V_AUDIO_DESCRIPTION                         = "V_AUDIO_DESCRIPTION";
    private static final String     http_300                                    = "http_300";
    private static final String     http_800                                    = "http_800";
    private static final String     http_1500                                   = "http_1500";
    private static final String     http_2200                                   = "http_2200";
    private static final String     LOAD_LANGUAGE_URL                           = "LOAD_LANGUAGE_URL";
    private static final String     LOAD_LANGUAGE_GERMAN                        = "LOAD_LANGUAGE_GERMAN";
    private static final String     LOAD_LANGUAGE_FRENCH                        = "LOAD_LANGUAGE_FRENCH";
    private static final String     THUMBNAIL                                   = "THUMBNAIL";
    private static final String     FAST_LINKCHECK                              = "FAST_LINKCHECK";

    private static final short      format_intern_german                        = 1;
    private static final short      format_intern_french                        = 2;
    private static final short      format_intern_subtitled                     = 3;
    private static final short      format_intern_subtitled_for_disabled_people = 4;
    private static final short      format_intern_audio_description             = 5;
    private static final short      format_intern_unknown                       = 6;

    final String[]                  formats                                     = { http_300, http_800, http_1500, http_2200 };

    private static final String     LANG_DE                                     = "de";
    private static final String     LANG_FR                                     = "fr";

    private short                   languageVersion                             = 1;
    private String                  parameter;
    private ArrayList<DownloadLink> decryptedLinks                              = new ArrayList<DownloadLink>();
    private String                  example_arte_vp_url                         = null;

    @SuppressWarnings("deprecation")
    public ArteMediathekDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    /** TODO: Re-Write parts of this - remove the bad language handling! */
    /*
     * E.g. smil (rtmp) url:
     * http://www.arte.tv/player/v2/webservices/smil.smil?json_url=http%3A%2F%2Farte.tv%2Fpapi%2Ftvguide%2Fvideos%2Fstream
     * %2Fplayer%2FD%2F045163-000_PLUS7-D%2FALL%2FALL.json&smil_entries=RTMP_SQ_1%2CRTMP_MQ_1%2CRTMP_LQ_1
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load host plugin to access some static methods later */
        JDUtilities.getPluginForHost("arte.tv");
        int foundFormatsNum = 0;
        parameter = param.toString();
        this.example_arte_vp_url = null;
        ArrayList<String> selectedLanguages = new ArrayList<String>();
        String title = getUrlFilename();
        String fid = null;
        String thumbnailUrl = null;
        final String plain_domain = new Regex(parameter, "([a-z]+\\.arte\\.tv)").getMatch(0);
        final String plain_domain_decrypter = plain_domain.replace("arte.tv", "artejd_decrypted_jd.tv");
        final SubConfiguration cfg = SubConfiguration.getConfig("arte.tv");
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String hybridAPIUrl = null;
        String date_formatted = "-";
        final boolean fastLinkcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);

        setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(503);
        br.getPage(parameter);
        try {
            if (this.br.getHttpConnection().getResponseCode() != 200 && this.br.getHttpConnection().getResponseCode() != 301) {
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }
            /* First we need to have some basic data - this part is link-specific. */
            if (parameter.matches(TYPE_CONCERT)) {
                if (!br.containsHTML("id=\"section\\-player\"")) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container\"")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                this.example_arte_vp_url = getArteVPUrl();
                if (this.example_arte_vp_url == null) {
                    return null;
                }
                fid = new Regex(this.example_arte_vp_url, "http://concert\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                hybridAPIUrl = "http://concert.arte.tv/%s/player/%s";
            } else if (parameter.matches(TYPE_CREATIVE)) {
                scanForExternalUrls();
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                fid = br.getRegex("\"http://creative\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                hybridAPIUrl = "http://creative.arte.tv/%s/player/%s";
            } else if (parameter.matches(TYPE_GUIDE)) {
                int status = br.getHttpConnection().getResponseCode();
                if (br.getHttpConnection().getResponseCode() == 400 || br.containsHTML("<h1>Error 404</h1>") || (!parameter.contains("tv/guide/") && status == 200)) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                /* new arte+7 handling */
                if (status != 200) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                /* Make sure not to download trailers or announcements to movies by grabbing the whole section of the videoplayer! */
                final String video_section = br.getRegex("(<section class=\\'focus\\' data-action=.*?</section>)").getMatch(0);
                if (video_section == null) {
                    return null;
                }
                this.example_arte_vp_url = getArteVPUrl(video_section);
                if (this.example_arte_vp_url == null) {
                    /* We cannot be entirely sure but no videourl == we have no video == offline link */
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                fid = new Regex(example_arte_vp_url, "/stream/player/[A-Za-z]{1,5}/([^<>\"/]*?)/").getMatch(0);
                if (fid == null) {
                    if (!br.containsHTML("arte_vp_config=")) {
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    /* Title is only available on DVD (buyable) */
                    if (video_section.contains("class='badge-vod'>VOD DVD</span>")) {
                        title = "only_available_on_DVD_" + title;
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    } else if (video_section.contains("class='badge-live'")) {
                        title = "livestreams_are_not_supported_" + title;
                        throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                    }
                    throw new DecrypterException("Decrypter broken: " + parameter);
                }
                /*
                 * first "ALL" can e.g. be replaced with "HBBTV" to only get the HBBTV qualities. Also possible:
                 * https://api.arte.tv/api/player/v1/config/fr/051939-015-A?vector=CINEMA
                 */
                hybridAPIUrl = "http://arte.tv/papi/tvguide/videos/stream/player/%s/%s/ALL/ALL.json";
            } else if (parameter.matches(TYPE_FUTURE)) {
                scanForExternalUrls();
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                fid = br.getRegex("\"http://future\\.arte\\.tv/[a-z]{2}/player/(\\d+)").getMatch(0);
                if (fid != null) {
                    hybridAPIUrl = "http://future.arte.tv/%s/player/%s";
                } else {
                    fid = br.getRegex("api\\.arte\\.tv/api/player/v1/config/(?:de|fr)/([A-Za-z0-9\\-]+)").getMatch(0);
                    hybridAPIUrl = "https://api.arte.tv/api/player/v1/config/%s/%s?vector=FUTURE&autostart=1";
                }
            } else if (parameter.matches(TYPE_CINEMA)) {
                scanForExternalUrls();
                if (decryptedLinks.size() > 0) {
                    return decryptedLinks;
                }
                if (!br.containsHTML("class=\"video\\-container\"")) {
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                example_arte_vp_url = getArteVPUrl();
                if (example_arte_vp_url == null) {
                    decryptedLinks.add(createofflineDownloadLink(parameter));
                    return decryptedLinks;
                }
                if (example_arte_vp_url.matches(API_TYPE_GUIDE)) {
                    /* Same API-urls as for "normal" arte.tv urls. Most likely used for complete movies. */
                    fid = new Regex(example_arte_vp_url, "/player/[^/]+/([A-Za-z0-9\\-_]+)").getMatch(0);
                    hybridAPIUrl = "http://arte.tv/papi/tvguide/videos/stream/player/%s/%s/ALL/ALL.json";
                } else {
                    fid = new Regex(example_arte_vp_url, "api\\.arte\\.tv/api/player/v1/config/(?:de|fr)/([A-Za-z0-9\\-]+)").getMatch(0);
                    final String vector = new Regex(example_arte_vp_url, "vector=([A-Za-z0-9]+)").getMatch(0);
                    hybridAPIUrl = "https://api.arte.tv/api/player/v1/config/%s/%s?vector=" + vector + "&autostart=1";
                }
            }
            if (fid == null) {
                return null;
            }
            /*
             * Now let's check which languages the user wants. We'll do the quality selection later but we have to access webpages to get
             * the different languages so let's keep the load low by only grabbing what the user selected.
             */
            if (cfg.getBooleanProperty(LOAD_LANGUAGE_URL, true)) {
                selectedLanguages.add(this.getUrlLang());
            } else {
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_GERMAN, true) && !selectedLanguages.contains(LANG_DE)) {
                    selectedLanguages.add(LANG_DE);
                }
                if (cfg.getBooleanProperty(LOAD_LANGUAGE_FRENCH, true) && !selectedLanguages.contains(LANG_FR)) {
                    selectedLanguages.add(LANG_FR);
                }
            }
            /* Finally, grab all we can get (in the selected language(s)) */
            for (final String selectedLanguage : selectedLanguages) {
                setSelectedLang_format_code(selectedLanguage);
                final String apiurl = this.getAPIUrl(hybridAPIUrl, selectedLanguage, fid);
                br.getPage(apiurl);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* In most cases this simply means that one of the selected languages is not available so let's go on. */
                    logger.info("This language is not available: " + selectedLanguage);
                    continue;
                }
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
                final LinkedHashMap<String, Object> videoJsonPlayer = (LinkedHashMap<String, Object>) entries.get("videoJsonPlayer");
                final Object error_info = videoJsonPlayer.get("custom_msg");
                if (error_info != null) {
                    final LinkedHashMap<String, Object> errorInfomap = (LinkedHashMap<String, Object>) error_info;
                    final String errmsg = (String) errorInfomap.get("msg");
                    final String type = (String) errorInfomap.get("type");
                    if ((type.equals("error") || type.equals("info")) && errmsg != null) {
                        title = errmsg + "_" + title;
                    } else {
                        title = "Unknown_error_" + title;
                        logger.warning("Unknown error");
                    }
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                // final String sourceURL = (String) videoJsonPlayer.get("VTR");
                /* Title is sometimes null e.g. for expired videos */
                final String json_title = (String) videoJsonPlayer.get("VTI");
                if (json_title != null) {
                    title = encodeUnicode(json_title);
                }
                String description = (String) videoJsonPlayer.get("VDE");
                if (description == null) {
                    description = (String) videoJsonPlayer.get("V7T");
                }
                final String errormessage = (String) entries.get("msg");
                if (errormessage != null) {
                    final DownloadLink offline = createofflineDownloadLink(parameter);
                    offline.setFinalFileName(title + errormessage);
                    offline.setComment(description);
                    ret.add(offline);
                    return ret;
                }
                if (thumbnailUrl == null) {
                    thumbnailUrl = (String) videoJsonPlayer.get("programImage");
                }
                final String vru = (String) videoJsonPlayer.get("VRU");
                final String vra = (String) videoJsonPlayer.get("VRA");
                final String vdb = (String) videoJsonPlayer.get("VDB");
                if ((vru != null && vra != null) || vdb != null) {
                    date_formatted = formatDate(vra);
                    /*
                     * In this case the video is not yet released and there usually is a value "VDB" which contains the release-date of the
                     * video --> But we don't need that - right now, such videos are simply offline and will be added as offline.
                     */
                    final String expired_message;
                    if (vdb != null) {
                        expired_message = String.format(jd.plugins.hoster.ArteTv.getPhrase("ERROR_CONTENT_NOT_AVAILABLE_YET"), jd.plugins.hoster.ArteTv.getNiceDate2(vdb));
                    } else {
                        expired_message = jd.plugins.hoster.ArteTv.getExpireMessage(selectedLanguage, convertDateFormat(vra), convertDateFormat(vru));
                    }
                    if (expired_message != null) {
                        final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                        link.setComment(description);
                        link.setProperty("offline", true);
                        link.setFinalFileName(expired_message + "_" + title);
                        decryptedLinks.add(link);
                        return decryptedLinks;
                    }
                }
                final Object vsro = videoJsonPlayer.get("VSR");
                if (!(vsro instanceof LinkedHashMap)) {
                    /* No source available --> Video cannot be played --> Browser would says "Error code 2" then */
                    throw new DecrypterException(EXCEPTION_LINKOFFLINE);
                }
                final Collection<Object> vsr_quals = ((LinkedHashMap<String, Object>) vsro).values();
                /* One packagename for every language */
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title);

                for (final Object o : vsr_quals) {
                    foundFormatsNum++;
                    final LinkedHashMap<String, Object> qualitymap = (LinkedHashMap<String, Object>) o;
                    final Object widtho = qualitymap.get("width");
                    final Object heighto = qualitymap.get("height");
                    String videoresolution = "";
                    String width = "";
                    String height = "";
                    final String protocol = "http";
                    final int videoBitrate = ((Number) qualitymap.get("bitrate")).intValue();
                    if (widtho != null && heighto != null) {
                        /* These parameters are available in 95+% of all cases! */
                        width = "" + ((Number) qualitymap.get("width")).intValue();
                        height = "" + ((Number) qualitymap.get("height")).intValue();
                        videoresolution = width + "x" + height;
                    }
                    final String versionCode = (String) qualitymap.get("versionCode");
                    final String versionLibelle = (String) qualitymap.get("versionLibelle");
                    final String versionShortLibelle = (String) qualitymap.get("versionShortLibelle");
                    final String url = (String) qualitymap.get("url");

                    final short format_code = getFormatCode(versionShortLibelle, versionCode);
                    final String quality_intern = protocol + "_" + videoBitrate;
                    final String filename = date_formatted + "_arte_" + title + "_" + get_user_language_from_format_code(format_code) + "_" + get_user_format_from_format_code(format_code) + "_" + versionCode + "_" + versionLibelle + "_" + versionShortLibelle + "_" + videoresolution + "_" + videoBitrate + ".mp4";

                    /* Lets check if we can add this link / if user wants it. */
                    /* Ignore HLS/RTMP versions */
                    if (!url.startsWith("http") || url.contains(".m3u8")) {
                        logger.info("Skipping " + filename + " because it is not a supported streaming format");
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_NORMAL, true) && (format_code == format_intern_german || format_code == format_intern_french)) {
                        /* User does not want the non-subtitled version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_SUBTITLED, true) && format_code == format_intern_subtitled) {
                        /* User does not want the subtitled version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_SUBTITLE_DISABLED_PEOPLE, true) && format_code == format_intern_subtitled_for_disabled_people) {
                        /* User does not want the subtitled-for-.disabled-people version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(V_AUDIO_DESCRIPTION, true) && format_code == format_intern_audio_description) {
                        /* User does not want the audio-description version */
                        continue;
                    }
                    if (!cfg.getBooleanProperty(quality_intern, true)) {
                        /* User does not want this bitrate --> Skip it */
                        logger.info("Skipping " + quality_intern);
                        continue;
                    }

                    final DownloadLink link = createDownloadlink("http://" + plain_domain_decrypter + "/" + System.currentTimeMillis() + new Random().nextInt(1000000000));

                    link.setFinalFileName(filename);
                    link.setContentUrl(parameter);
                    link._setFilePackage(fp);
                    link.setProperty("directURL", url);
                    link.setProperty("directName", filename);
                    link.setProperty("quality_intern", quality_intern);
                    link.setProperty("langShort", selectedLanguage);
                    link.setProperty("mainlink", parameter);
                    link.setProperty("apiurl", apiurl);
                    if (vra != null && vru != null) {
                        link.setProperty("VRA", convertDateFormat(vra));
                        link.setProperty("VRU", convertDateFormat(vru));
                    }
                    link.setComment(description);
                    link.setContentUrl(parameter);
                    /* Use filename as linkid as it is unique! */
                    link.setLinkID(filename);
                    if (fastLinkcheck) {
                        link.setAvailable(true);
                    }
                    decryptedLinks.add(link);
                }
            }

            /* User did not activate all versions --> Show this info in filename so he can correct his mistake. */
            if (decryptedLinks.isEmpty() && foundFormatsNum > 0) {
                title = jd.plugins.hoster.ArteTv.getPhrase("ERROR_USER_NEEDS_TO_CHANGE_FORMAT_SELECTION") + title;
                throw new DecrypterException(EXCEPTION_LINKOFFLINE);
            }

            /* Check if user wants to download the thumbnail as well. */
            if (cfg.getBooleanProperty(THUMBNAIL, true) && thumbnailUrl != null) {
                final DownloadLink link = createDownloadlink("directhttp://" + thumbnailUrl);
                link.setFinalFileName(title + ".jpg");
                decryptedLinks.add(link);
            }
            if (decryptedLinks.size() > 1) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(date_formatted + "_arte_" + title);
                fp.addLinks(decryptedLinks);
            }
        } catch (final Exception e) {
            if (e instanceof DecrypterException && e.getMessage().equals(EXCEPTION_LINKOFFLINE)) {
                final DownloadLink offline = createofflineDownloadLink(parameter);
                offline.setFinalFileName(title);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            throw e;
        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private void scanForExternalUrls() {
        /* Return external links if existant */
        final String currentHost = new Regex(this.br.getURL(), "https?://([^/]*?)/.+").getMatch(0);
        final String[] externURLsRegexes = { "data\\-url=\"(http://creative\\.arte\\.tv/(de|fr)/scald_dmcloud_json/\\d+)", "(youtube\\.com/embed/[^<>\"]*?)\"" };
        for (final String externURLRegex : externURLsRegexes) {
            final String[] externURLs = br.getRegex(externURLRegex).getColumn(0);
            if (externURLs != null && externURLs.length > 0) {
                for (String externURL : externURLs) {
                    if (externURL.matches("youtube\\.com/embed/.+")) {
                        externURL = "https://" + externURL;
                    } else if (!externURL.startsWith("http")) {
                        /* TODO: http://cinema.arte.tv/fr/magazine/court-circuit */
                        externURL = "http://" + currentHost + externURL;
                    }
                    final DownloadLink dl = createDownloadlink(externURL);
                    decryptedLinks.add(dl);
                }
            }
        }
    }

    private String getArteVPUrl() {
        return getArteVPUrl(this.br.toString());
    }

    private String getArteVPUrl(final String source) {
        return new Regex(source, "arte_vp_url=(?:\"|\\')(http[^<>\"\\']*?)(?:\"|\\')").getMatch(0);
    }

    /* Collection of possible values */
    private final String[] versionCodes             = { "VO", "VO-STA", "VOF-STMF", "VA-STMA", "VOF-STA", "VOA-STA", "VOA-STMA", "VAAUD", "VE", "VF-STMF", "VE[ANG]", "VI", "VO-STE[ANG]", "VO-STE[ESP]", "VO-STE[ITA]", "VO-STE[POL]", "VOA-STE[ESP]", "VOA-STE[ANG]" };
    /* Can also be "-" */
    private final String[] versionShortLibelleCodes = { "DE", "VA", "VE", "FR", "VF", "OmU", "VO", "VOA", "VOF", "VOSTF", "VE[ANG]", "VI", "OmU-ANG", "OmU-ESP", "OmU-ITA", "OmU-POL" };

    /* Non-subtitled versions, 3 = Subtitled versions, 4 = Subtitled versions for disabled people, 5 = Audio descriptions, 6 = unknown */
    private short getFormatCode(final String versionShortLibelle, final String versionCode) throws DecrypterException {
        /* versionShortLibelle: What is UTH?? */
        /* versionCode: VO is not necessarily french */
        if (versionShortLibelle == null || versionCode == null) {
            throw new DecrypterException("Decrypter broken");
        }
        short lint;
        if (versionCode.equals("VO") && parameter.matches(TYPE_CONCERT)) {
            /* Special case - no different versions available --> We already got the version we want */
            lint = languageVersion;
        } else if ("VOF-STA".equalsIgnoreCase(versionCode) || "VOF-STMF".equals(versionCode) || "VA-STMA".equals(versionCode)) {
            lint = format_intern_subtitled;
        } else if (versionCode.equals("VOA-STMA")) {
            lint = format_intern_subtitled_for_disabled_people;
        } else if (versionCode.equals("VAAUD")) {
            lint = format_intern_audio_description;
        } else if (versionShortLibelle.equals("OmU") || versionShortLibelle.equals("VO") || versionCode.equals("VO") || versionShortLibelle.equals("VE") || versionCode.equals("VE") || versionShortLibelle.equals("VE[ANG]") || versionCode.equals("VE[ANG]") || versionCode.equals("VI") || "VOA-STA".equals(versionCode) || "VO-STE[ANG]".equals(versionCode) || versionCode.equals("VO-STE[ITA]") || versionCode.equals("VOA-STE[ESP]") || versionCode.equals("VOA-STE[ANG]")) {
            /* VE Actually means English but there is no specified selection for this. */
            /* Without language --> So it simply is our current language */
            lint = languageVersion;
        } else if (versionShortLibelle.equals("DE") || versionShortLibelle.equals("VA") || versionCode.equals("VO-STA") || versionShortLibelle.equals("VOSTA")) {
            lint = format_intern_german;
        } else if (versionShortLibelle.equals("FR") || versionShortLibelle.equals("VF") || versionShortLibelle.equals("VOF") || versionShortLibelle.equals("VOSTF") || versionCode.equals("VF-STMF")) {
            lint = format_intern_french;
        } else {
            /* Unknown */
            lint = format_intern_unknown;
        }
        return lint;
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    public static String get_user_format_from_format_code(final short version) {
        switch (version) {
        case format_intern_german:
            return "no_subtitle";
        case format_intern_french:
            return "no_subtitle";
        case format_intern_subtitled:
            return "subtitled";
        case format_intern_subtitled_for_disabled_people:
            return "subtitled_handicapped";
        case format_intern_audio_description:
            return "audio_description";
        case format_intern_unknown:
            return "no_subtitle";
        default:
            /* Obviously this should never happen */
            return "WTF_PLUGIN_FAILED";
        }
    }

    /* 1 = No subtitle, 3 = Subtitled version, 4 = Subtitled version for disabled people, 5 = Audio description */
    public static String get_user_language_from_format_code(final short version) {
        switch (version) {
        case format_intern_german:
            return "german";
        case format_intern_french:
            return "french";
        default:
            return "french";
        }
    }

    /**
     * Inout: Normal formatCode Output: formatCode for internal use (1+2 = 1) 1=German, 2 = French, both no_subtitle --> We only need the
     * 'no subtitle' information which has the code 1.
     */
    private int get_intern_format_code_from_format_code(final int formatCode) {
        if (formatCode == format_intern_german || formatCode == format_intern_french) {
            return 1;
        } else {
            return formatCode;
        }
    }

    private void setSelectedLang_format_code(final String short_lang) {
        if ("de".equals(short_lang)) {
            this.languageVersion = format_intern_german;
        } else {
            this.languageVersion = format_intern_french;
        }
    }

    private String getAPIUrl(final String hybridAPIlink, final String lang, final String id) {
        String apilink;
        if (example_arte_vp_url != null && this.example_arte_vp_url.matches(API_TYPE_GUIDE)) {
            final String api_language = this.artetv_api_language(lang);
            final String id_without_lang = id.substring(0, id.length() - 1);
            final String id_with_lang = id_without_lang + api_language;
            apilink = String.format(hybridAPIlink, api_language, id_with_lang);
        } else {
            apilink = String.format(hybridAPIlink, lang, id);
        }
        return apilink;
    }

    private String artetv_api_language() {
        return artetv_api_language(getUrlLang());
    }

    private String artetv_api_language(final String lang) {
        String apilang;
        if ("de".equals(lang)) {
            apilang = "D";
        } else {
            apilang = "F";
        }
        return apilang;
    }

    private String getUrlFilename() {
        String urlfilename;
        urlfilename = new Regex(parameter, "([A-Za-z0-9\\-]+)$").getMatch(0);
        return urlfilename;
    }

    private DownloadLink createofflineDownloadLink(final String parameter) {
        final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        return offline;
    }

    private String convertDateFormat(String s) {
        if (s == null) {
            return null;
        }
        if (s.matches("\\d+/\\d+/\\d+ \\d+:\\d+:\\d+ \\+\\d+")) {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.getDefault());
            SimpleDateFormat convdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            try {
                Date date = null;
                try {
                    date = df.parse(s);
                    s = convdf.format(date);
                } catch (Throwable e) {
                    df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z", Locale.ENGLISH);
                    date = df.parse(s);
                    s = convdf.format(date);
                }
            } catch (Throwable e) {
                return s;
            }
        }
        return s;
    }

    @SuppressWarnings("unused")
    private String getURLFilename(final String parameter) {
        String urlfilename;
        if (parameter.matches(TYPE_CONCERT)) {
            urlfilename = new Regex(parameter, "concert\\.arte\\.tv/(de|fr)/(.+)").getMatch(1);
        } else {
            urlfilename = new Regex(parameter, "arte\\.tv/guide/[a-z]{2}/(.+)").getMatch(0);
        }
        return urlfilename;
    }

    private String getUrlLang() {
        final String lang = new Regex(parameter, ".+(?:[a-z]+\\.arte\\.tv|/guide)/(\\w+)/.+").getMatch(0);
        return lang;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy HH:mm:ss Z", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}