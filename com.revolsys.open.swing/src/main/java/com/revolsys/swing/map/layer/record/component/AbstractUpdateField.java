package com.revolsys.swing.map.layer.record.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import com.revolsys.record.schema.FieldDefinition;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.action.enablecheck.EnableCheck;
import com.revolsys.swing.component.BasePanel;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.table.RecordLayerTable;
import com.revolsys.swing.map.layer.record.table.model.RecordLayerTableModel;
import com.revolsys.swing.menu.Menus;
import com.revolsys.swing.table.TablePanel;

public abstract class AbstractUpdateField extends JDialog {
  private static final long serialVersionUID = 1L;

  protected static EnableCheck newEnableCheck() {
    final EnableCheck enableCheck = Menus.enableCheck((final RecordLayerTable table) -> {
      final int columnIndex = TablePanel.getEventColumn();
      final RecordLayerTableModel tableModel = table.getModel();
      if (tableModel.isFieldEditable(columnIndex)) {
        if (tableModel.isIdField(columnIndex)) {
          return false;
        } else {
          return true;
        }
      } else {
        return false;
      }
    });
    return enableCheck;
  }

  private final FieldDefinition fieldDefinition;

  private final DecimalFormat format = new DecimalFormat("#,###");

  private final AbstractRecordLayer layer;

  private JButton okButton;

  private final int recordCount;

  private final RecordLayerTable table;

  private final RecordLayerTableModel tableModel;

  private final String recordCountString;

  protected AbstractUpdateField(final String title) {
    super(SwingUtil.getActiveWindow(), title, ModalityType.APPLICATION_MODAL);
    this.table = TablePanel.getEventTable();
    this.tableModel = this.table.getModel();
    this.layer = this.table.getLayer();
    final int eventColumn = TablePanel.getEventColumn();
    this.fieldDefinition = this.table.getColumnFieldDefinition(eventColumn);
    this.recordCount = this.tableModel.getRowCount();
    this.recordCountString = this.format.format(this.recordCount);
    initDialog();

  }

  protected void cancel() {
    if (isVisible()) {
      SwingUtil.dispose(this);
    }
  }

  private void finish() {
    if (this.recordCount > 100) {
      final int confirm = JOptionPane.showConfirmDialog(this,
        "<html>Update <b style='color:#32CD32'>" + this.recordCountString
          + "</b> records? This may take a long time or fail if there are many records.</html>",
        "Update Records?", JOptionPane.OK_CANCEL_OPTION);
      if (confirm != JOptionPane.OK_OPTION) {
        setVisible(false);
        return;
      }
    }
    final ProgressMonitor progressMonitor = new ProgressMonitor(this, getProgressMonitorTitle(),
      "Updated 0 of " + this.recordCountString, 0, 100);
    progressMonitor.setProgress(0);
    setVisible(false);
    final AtomicInteger progress = new AtomicInteger();
    final SwingWorker<?, ?> task = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        AbstractUpdateField.this.tableModel.forEachRecord((record) -> {
          if (progressMonitor.isCanceled()) {
            throw new CancellationException();
          } else {
            updateRecord(record);
            final int updateCount = progress.incrementAndGet();
            if (updateCount < AbstractUpdateField.this.recordCount) {
              final int updatePercent = (int)Math
                .floor(updateCount * 100 / (double)AbstractUpdateField.this.recordCount);
              if (updatePercent < 100) {
                setProgress(updatePercent);
              }
            }
          }
        });
        setProgress(100);
        return null;
      }

      @Override
      protected void done() {
      }

    };
    task.addPropertyChangeListener((event) -> {
      if ("progress" == event.getPropertyName()) {
        final int percent = (Integer)event.getNewValue();
        progressMonitor.setProgress(percent);
        progressMonitor.setNote("Updated " + progress + " of " + this.recordCountString);
      }
    });
    task.execute();
  }

  protected FieldDefinition getFieldDefinition() {
    return this.fieldDefinition;
  }

  public AbstractRecordLayer getLayer() {
    return this.layer;
  }

  protected String getProgressMonitorTitle() {
    return "Set " + this.fieldDefinition.getName();
  }

  public String getRecordCountString() {
    return this.recordCountString;
  }

  protected void initDialog() {
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setMinimumSize(new Dimension(300, 100));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        cancel();
      }
    });

    final BasePanel panel = new BasePanel(new BorderLayout());
    add(panel);

    final JPanel fieldPanel = initFieldPanel();
    panel.add(fieldPanel, BorderLayout.CENTER);

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    add(buttonsPanel, BorderLayout.SOUTH);

    final JButton cancelButton = RunnableAction.newButton("Cancel", this::cancel);
    buttonsPanel.add(cancelButton);

    this.okButton = RunnableAction.newButton("OK", this::finish);
    if (this.fieldDefinition.isRequired()) {
      this.okButton.setEnabled(false);
    }
    buttonsPanel.add(this.okButton);

    pack();
    SwingUtil.autoAdjustPosition(this);
  }

  protected abstract JPanel initFieldPanel();

  protected void setFormValid(final boolean valid) {
    this.okButton.setEnabled(valid);
  }

  protected abstract void updateRecord(final LayerRecord record);

}