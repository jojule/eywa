package org.vaadin.eywa.demo;

import org.vaadin.eywa.EywaProperty;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Title;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.BorderStyle;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Link;
import com.vaadin.ui.Slider;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Title("Eywa Shared Datasource Add-on Demo")
@SuppressWarnings("serial")
@Push
public class DemoUI extends UI {

    @Override
    protected void init(VaadinRequest request) {

        VerticalLayout center = new VerticalLayout();
        setContent(center);
        center.setSizeFull();

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeUndefined();
        center.addComponent(layout);
        center.setComponentAlignment(layout, Alignment.MIDDLE_CENTER);

        EywaProperty<String> p = new EywaProperty<String>("shared-textfield",
                String.class);
        if (p.getValue() == null) {
            p.setValue("Share text area");
        }
        final TextArea ta = new TextArea(p);
        ta.setWidth("300px");
        ta.setHeight("200px");
        layout.addComponent(ta);
        ta.setTextChangeEventMode(TextChangeEventMode.EAGER);
        ta.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                ta.setValue(event.getText());
            }
        });

        final Slider slider = new Slider(0.0, 100.0, 1);
        slider.setPropertyDataSource(new EywaProperty<Double>("shared-slider",
                Double.class));
        layout.addComponent(slider);
        slider.setImmediate(true);
        layout.addComponent(slider);
        slider.setWidth("300px");

        layout.addComponent(new Link("Open this in new window",
                new ExternalResource(getPage().getLocation().toString()), ""
                        + System.currentTimeMillis(), 400, 300,
                BorderStyle.MINIMAL));
    }

}
