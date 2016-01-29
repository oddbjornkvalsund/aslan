package no.nixx.aslan.ui.components;

import java.util.ArrayList;
import java.util.Collections;

public class Line extends ArrayList<Fragment> implements BufferItem {

    private static final long serialVersionUID = 1361069937007855282L;

    public Line(Fragment... fragments) {
        Collections.addAll(this, fragments);
    }
}