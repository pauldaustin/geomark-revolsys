package com.revolsys.swing.map.layer.record;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JMenu;

import com.revolsys.record.Record;
import com.revolsys.swing.Icons;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.menu.MenuFactory;

public class EditRecordMenu extends MenuFactory {

  private final boolean singleRecord;

  private AbstractRecordLayer layer;

  private List<LayerRecord> records = Collections.emptyList();

  public EditRecordMenu(final boolean singleRecord) {
    this.singleRecord = singleRecord;
    if (singleRecord) {
      setName("Record Operations");
    } else {
      setName("Selected Records");
    }
  }

  public <R extends Record> RunnableAction addMenuItemRecord(final String groupName,
    final CharSequence name, final String iconName, final Consumer<R> consumer) {
    return addMenuItemRecord(groupName, -1, name, null, iconName, null, consumer);
  }

  public <R extends Record> RunnableAction addMenuItemRecord(final String groupName,
    final CharSequence name, final String iconName, final EnableCheck enableCheck,
    final Consumer<R> action) {
    return addMenuItemRecord(groupName, -1, name, null, iconName, enableCheck, action);
  }

  public <R extends Record> RunnableAction addMenuItemRecord(final String groupName,
    final int index, final CharSequence name, final String toolTip, final String iconName,
    final EnableCheck enableCheck, final Consumer<R> action) {

    final BiConsumer<AbstractRecordLayer, List<LayerRecord>> recordsAction = (layer,
      records) -> layer.processRecords(name, records, action);
    return addMenuItemRecords(groupName, index, name, toolTip, iconName, enableCheck,
      recordsAction);
  }

  public <L extends AbstractRecordLayer, R extends LayerRecord> RunnableAction addMenuItemRecords(
    final String groupName, final CharSequence name, final String iconName,
    final BiConsumer<L, List<R>> recordsAction) {
    return addMenuItemRecords(groupName, -1, name, null, iconName, null, recordsAction);
  }

  public <L extends AbstractRecordLayer, R extends LayerRecord> RunnableAction addMenuItemRecords(
    final String groupName, final CharSequence name, final String iconName,
    final EnableCheck enableCheck, final BiConsumer<L, List<R>> recordsAction) {
    return addMenuItemRecords(groupName, -1, name, null, iconName, enableCheck, recordsAction);
  }

  public <L extends AbstractRecordLayer, R extends LayerRecord> RunnableAction addMenuItemRecords(
    final String groupName, final int index, final CharSequence name, final String toolTip,
    final String iconName, final EnableCheck enableCheck,
    final BiConsumer<L, List<R>> recordsAction) {
    final Icon icon = Icons.getIcon(iconName);
    // Cache the values of these two fields at the time the menu was created
    @SuppressWarnings("unchecked")
    final L layer = (L)this.layer;
    @SuppressWarnings("unchecked")
    final List<R> records = (List<R>)this.records;
    final RunnableAction action = MenuFactory.newMenuItem(name, toolTip, icon, enableCheck,
      () -> recordsAction.accept(layer, records));
    addComponentFactory(groupName, index, action);
    return action;
  }

  @Override
  public MenuFactory clone() {
    return new EditRecordMenu(this.singleRecord);
  }

  @Override
  public JMenu newComponent() {
    try {
      if (this.singleRecord) {
        final LayerRecord record = LayerRecordMenu.getEventRecord();
        if (record != null) {
          this.layer = record.getLayer();
          this.records = Collections.singletonList(record);
        }
      } else {
        final Object menuSource = MenuFactory.getMenuSource();
        if (menuSource instanceof AbstractRecordLayer) {
          this.layer = (AbstractRecordLayer)menuSource;
          this.records = this.layer.getSelectedRecords();
        }
      }
      clear();
      if (this.layer != null && !this.layer.isDeleted() && !this.records.isEmpty()) {
        setEnableCheck(EnableCheck.ENABLED);
        this.layer.initEditRecordsMenu(this, this.records);
      } else {
        setEnableCheck(EnableCheck.DISABLED);
      }

      return super.newComponent();
    } finally {
      this.layer = null;
      this.records = Collections.emptyList();
    }
  }
}
