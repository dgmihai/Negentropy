package com.trajan.negentropy.client;

import com.trajan.negentropy.client.components.grid.ColumnKey;
import com.trajan.negentropy.client.components.grid.GridUtil;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.model.Tenet;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@PageTitle("Negentropy - Tenets")
@UIScope
@Route(value = "tenets", layout = MainLayout.class)
@Uses(Icon.class)
public class TenetView extends VerticalLayout {
    @Autowired private BannerProvider bannerProvider;
    @Autowired private SessionServices services;

    @PostConstruct
    public void init() {
        this.addClassName("tenet-view");
        this.setSizeFull();

        Grid<Tenet> grid = new Grid<>(Tenet.class, false);
        grid.setItems(services.tenet().getAll().toList());

        Grid.Column<Tenet> tenetColumn = grid.addColumn(Tenet::body)
                .setAutoWidth(true)
                .setHeader("Tenet")
                .setFlexGrow(1);

        Editor<Tenet> editor = grid.getEditor();
        Binder<Tenet> binder = new Binder<>(Tenet.class);
        editor.setBinder(binder);
        editor.setBuffered(true);
        editor.addSaveListener(tenet -> {
            services.tenet().persist(tenet.getItem());
            grid.setItems(services.tenet().getAll().toList());
        });

        TextField editorField = new TextField();
        editorField.setWidthFull();
        binder.forField(editorField)
                .asRequired("Cannot be empty!")
                .bind(Tenet::body, Tenet::body);

        tenetColumn.setEditorComponent(editorField);

        TextField addField = new TextField();
        addField.setPlaceholder("Add new tenet");
        addField.setWidthFull();
        addField.addKeyPressListener(Key.ENTER, e -> {
            if (!addField.isEmpty()) {
                Tenet tenet = new Tenet(addField.getValue());
                services.tenet().persist(tenet);
                addField.clear();
                grid.setItems(services.tenet().getAll().toList());
            }
        });
        addField.addKeyPressListener(Key.ESCAPE, e -> {
            if (!addField.isEmpty()) {
                addField.clear();
            }
        });

        Grid.Column<Tenet> editColumn = grid.addColumn(LitRenderer.<Tenet>of(
                                GridUtil.inlineVaadinIconLitExpression("edit",
                                        "active"))
                        .withFunction("onClick", t -> {
                            if (editor.isOpen()) {
                                editor.cancel();
                            }
                            editor.editItem(t);
                        }))
                .setKey(ColumnKey.EDIT.toString())
                .setWidth(GridUtil.ICON_COL_WIDTH_XL)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
        check.addClickListener(e -> editor.save());
        editColumn.setEditorComponent(check);

        AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());

        editor.addOpenListener(e -> {
            escapeListener.get().ifPresent(Registration::remove);
            enterListener.set(Optional.of(Shortcuts.addShortcutListener(grid,
                    editor::save,
                    Key.ENTER)));
        });

        editor.addCloseListener(e -> {
            enterListener.get().ifPresent(Registration::remove);
            escapeListener.set(Optional.of(Shortcuts.addShortcutListener(grid,
                    editor::cancel,
                    Key.ESCAPE)));
        });

        grid.addColumn(LitRenderer.<Tenet>of(
                                GridUtil.inlineVaadinIconLitExpression("trash",
                                        " delete"))
                        .withFunction("onClick", tenet -> {
                            services.tenet().delete(tenet.id());
                            grid.setItems(services.tenet().getAll().toList());
                        }))
                .setKey(ColumnKey.DELETE.toString())
                .setHeader(GridUtil.headerIcon(VaadinIcon.TRASH))
                .setWidth(GridUtil.ICON_COL_WIDTH_XL)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        this.add(addField);
        this.add(grid);
    }
}
