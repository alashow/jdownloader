package org.jdownloader.gui.views.downloads.columns;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.columnmenu.LockColumnWidthAction;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.download.DownloadInterface;

public class SpeedColumn extends ExtTextColumn<AbstractNode> {

    /**
     *
     */
    private static final long   serialVersionUID    = 1L;
    private final AtomicBoolean warningEnabled      = new AtomicBoolean(false);
    private final Icon          warningIcon;
    private final AtomicBoolean speedLimiterEnabled = new AtomicBoolean(false);
    private final Color         defaultColor;

    public SpeedColumn() {
        super(_GUI._.SpeedColumn_SpeedColumn());
        rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
        warningEnabled.set(CFG_GUI.PREMIUM_ALERT_SPEED_COLUMN_ENABLED.isEnabled());
        CFG_GUI.PREMIUM_ALERT_SPEED_COLUMN_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                warningEnabled.set(Boolean.TRUE.equals(newValue));
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        warningIcon = new AbstractIcon(IconKey.ICON_WARNING, 16);
        defaultColor = rendererField.getForeground();
        speedLimiterEnabled.set(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.isEnabled());
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                speedLimiterEnabled.set(Boolean.TRUE.equals(newValue));
            }
        }, false);
    }

    public JPopupMenu createHeaderPopup() {
        final JPopupMenu ret = new JPopupMenu();
        LockColumnWidthAction action;
        ret.add(new JCheckBoxMenuItem(action = new LockColumnWidthAction(this)));

        ret.add(new JCheckBoxMenuItem(new AppAction() {
            {
                setName(_GUI._.literall_premium_alert());
                setSmallIcon(new AbstractIcon(IconKey.ICON_WARNING, 16));
                setSelected(JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertSpeedColumnEnabled());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                JsonConfig.create(GraphicalUserInterfaceSettings.class).setPremiumAlertSpeedColumnEnabled(!JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertSpeedColumnEnabled());
            }
        }));
        ret.add(new JSeparator());
        return ret;
    }

    @Override
    public void configureRendererComponent(AbstractNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.configureRendererComponent(value, isSelected, hasFocus, row, column);
        if (speedLimiterEnabled.get()) {
            rendererField.setForeground(Color.RED);
        } else {
            rendererField.setForeground(defaultColor);
        }
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getDefaultWidth() {
        return 130;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (isSpeedWarning(value)) {
            return warningIcon;
        }
        return null;
    }

    private boolean isSpeedWarning(AbstractNode value) {
        if (warningEnabled.get() && value instanceof DownloadLink) {
            final DownloadLink dl = (DownloadLink) value;
            final SingleDownloadController dlc = dl.getDownloadLinkController();
            Account acc = null;
            if (dlc == null || (acc = dlc.getAccount()) != null) {
                return false;
            }
            PluginForHost plugin = dl.getDefaultPlugin();
            if (plugin == null || !plugin.isSpeedLimited(dl, acc) | !plugin.isPremiumEnabled()) {
                /* no account support yet for this plugin */
                return false;
            }
            long limit = dlc.getConnectionHandler().getLimit();
            if (limit > 0 && limit < 50 * 1024) {
                /* we have an active limit that is smaller than our warn speed */
                return false;
            }
            DownloadInterface dli = dlc.getDownloadInstance();
            if (dli != null && ((DownloadLink) value).getView().getBytesLoaded() > 100 * 1024) {
                if (((DownloadLink) value).getView().getSpeedBps() < 50 * 1024) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        final String ret = super.getTooltipText(obj);
        if (speedLimiterEnabled.get()) {
            final String limit = _GUI._.SpeedMeterPanel_getString_limited(SizeFormatter.formatBytes(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.getValue()));
            return limit + "\r\n" + ret;
        }
        return ret;
    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode obj) {
        if (isSpeedWarning(obj)) {
            try {
                Dialog.getInstance().showDialog(new PremiumInfoDialog((((DownloadLink) obj).getDomainInfo()), _GUI._.SpeedColumn_onSingleClick_object_(((DownloadLink) obj).getHost()), "SpeedColumn") {
                    protected String getDescription(DomainInfo info2) {
                        return _GUI._.SpeedColumn_getDescription_object_(info2.getTld());
                    }
                });
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            final PluginProgress pluginProgress = ((DownloadLink) value).getPluginProgress();
            if (pluginProgress instanceof DownloadPluginProgress) {
                long speed = ((DownloadPluginProgress) pluginProgress).getSpeed();
                if (speed > 0) {
                    return Formatter.formatReadable(speed) + "/s";
                }
            }
        } else if (value instanceof FilePackage) {
            final long speed = DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage((FilePackage) value);
            if (speed >= 0) {
                return Formatter.formatReadable(speed) + "/s";
            }
        }
        return null;
    }
}
