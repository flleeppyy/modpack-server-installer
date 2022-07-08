package com.flleeppyy.serverinstaller.JComponents;

import com.flleeppyy.serverinstaller.Json.ModpackInfo;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ModpackList extends JList implements SwingConstants {
    private Map<ModpackInfo, ImageIcon> packiconmap;

    public ModpackList() {
        super();
        setCellRenderer(new ModpackListRenderer());
        packiconmap = null;
    }

    public ModpackList(ModpackInfo[] modpackList) {
        super(modpackList);
        packiconmap = createPackMap(modpackList);
        setCellRenderer(new ModpackListRenderer());
    }
    
    public void setListData(ModpackInfo[] listData) {
        super.setListData(listData);
        packiconmap = createPackMap(listData);
    }

    public class ModpackListRenderer extends DefaultListCellRenderer {
        Font font = getFont().deriveFont(14f);

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            ImageIcon icon = packiconmap.get((ModpackInfo) value);
            label.setIcon(icon);
            label.setHorizontalTextPosition(JLabel.RIGHT);
            label.setFont(font);

            label.setPreferredSize(
                    new Dimension(
                            -1,
                            icon.getIconHeight()
                    )
            );
            return label;
        }
    }

    private Map<ModpackInfo, ImageIcon> createPackMap(ModpackInfo[] list) {
        Map<ModpackInfo, ImageIcon> map = new HashMap<>();
        for (ModpackInfo s : list) {
            ImageIcon icon = new ImageIcon(s.iconBytes);
            map.put(s, icon);
            System.out.println(icon.getIconWidth() + "" + icon.getIconHeight() + "" + icon.getImage().getWidth(null) + "" + icon.getImage().getHeight(null));
        }
        return map;
    }

}

