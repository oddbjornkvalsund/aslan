package no.nixx.wing.ui;

import javafx.scene.control.Tab;

public class WingTab extends Tab{
    public WingTab(String title) {
        setText(title);
        setContent(new WingShell());
    }
}