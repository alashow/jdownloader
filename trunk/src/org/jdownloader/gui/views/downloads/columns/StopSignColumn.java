package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;

public class StopSignColumn extends ExtTextColumn<AbstractNode> {

    private Icon icon;

    public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

        final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

            private static final long serialVersionUID = 2051980596953422289L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(new AbstractIcon(IconKey.ICON_STOPSIGN, 14));
                setHorizontalAlignment(CENTER);
                setText(null);
                return this;
            }
        };

        return ret;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return false;
    }

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public StopSignColumn() {
        super(_GUI._.StopSignColumn_StopSignColumn());
        icon = new AbstractIcon(IconKey.ICON_STOPSIGN, 16);
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (DownloadWatchDog.getInstance().getSession().isStopMark(value)) {
            return icon;
        }
        return null;
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        if (DownloadWatchDog.getInstance().getSession().isStopMark(obj)) {
            return _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_stopmark();
        }
        return null;
    }

    @Override
    public int getDefaultWidth() {
        return 30;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getMinWidth() {
        return getDefaultWidth();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        return "";
    }

}
