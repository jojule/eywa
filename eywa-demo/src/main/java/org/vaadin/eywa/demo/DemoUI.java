package org.vaadin.eywa.demo;

import org.vaadin.eywa.EywaProperty;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;

@Theme("demo")
@Title("Eywa Add-on Demo")
@SuppressWarnings("serial")
@Push
public class DemoUI extends UI
{
    EywaProperty<String> p = new EywaProperty<String>("shared", String.class);
    TextArea ta = new TextArea(p);

    @Override
    protected void init(VaadinRequest request) {

        ta.setSizeFull();
        ta.setTextChangeEventMode(TextChangeEventMode.EAGER);
        ta.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                ta.setValue(event.getText());
            }
        });
        setContent(ta);

    }

}
