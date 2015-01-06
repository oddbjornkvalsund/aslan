package no.nixx.wing.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;


public class WingMenuBar extends javafx.scene.control.MenuBar {
    public WingMenuBar() {
        final Menu newMenu = createNewMenu();
        final Menu editMenu = new Menu("Edit");
        final Menu viewMenu = new Menu("View");
        final Menu aboutMenu = new Menu("About");

        getMenus().addAll(newMenu, editMenu, viewMenu, aboutMenu);
    }

    private Menu createNewMenu() {
        final Menu newMenu = new Menu("New");
        final MenuItem newTabMenuItem = new MenuItem("New tab");
        final MenuItem newWindowMenuItem = new MenuItem("New window");
        newMenu.getItems().addAll(newTabMenuItem, newWindowMenuItem);

        // TODO: onAction -> fireEvent

        return newMenu;
    }
}