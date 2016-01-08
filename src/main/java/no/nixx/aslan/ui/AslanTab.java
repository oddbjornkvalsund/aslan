package no.nixx.aslan.ui;

import javafx.scene.control.Tab;

public class AslanTab extends Tab {
    public AslanTab(String title) {
        setText(title);
        setContent(new AslanShell2());
    }
}