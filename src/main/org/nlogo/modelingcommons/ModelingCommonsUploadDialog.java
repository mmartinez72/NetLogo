package org.nlogo.modelingcommons;

import org.nlogo.awt.UserCancelException;
import org.nlogo.swing.FileDialog;
import org.nlogo.swing.ModalProgressTask;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class ModelingCommonsUploadDialog extends JDialog {
  private JPanel contentPane;
  private JButton uploadModelButton;
  private JButton cancelButton;
  private JButton logoutButton;
  private JTextField modelNameField;
  private JLabel errorLabel;
  private JLabel personNameLabel;
  private JComboBox groupComboBox;
  private DisablableComboBox visibilityComboBox;
  private DisablableComboBox changeabilityComboBox;
  private JRadioButton useCurrentViewRadioButton;
  private JRadioButton autoGenerateViewRadioButton;
  private JRadioButton noPreviewRadioButton;
  private JRadioButton imageFromFileRadioButton;
  private JButton selectFileButton;
  private JLabel selectedFileLabel;
  private ButtonGroup previewImageButtonGroup;
  private ModelingCommons communicator;
  private Frame frame;
  private int groupPermissionIndex;
  private int userPermissionIndex;
  private int everyonePermissionIndex;
  private String uploadImageFilePath;
  ModelingCommonsUploadDialog(final Frame frame, final ModelingCommons communicator, String errorLabelText) {
    super(frame, "Upload Model to Modeling Commons", true);
    this.communicator = communicator;
    this.frame = frame;
    errorLabel.setText(errorLabelText);
    personNameLabel.setText("Hello " + communicator.getPerson().getFirstName() + " " + communicator.getPerson().getLastName());
    setSize(500, 500);
    setResizable(false);

    setContentPane(contentPane);
    setModal(true);
    getRootPane().setDefaultButton(uploadModelButton);

    uploadModelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onOK();
      }
    });

    cancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    });

    logoutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
        ModalProgressTask.apply(org.nlogo.awt.Hierarchy.getFrame(ModelingCommonsUploadDialog.this), "Logging out of Modeling Commons", new Runnable() {
          public void run() {
            communicator.logout();
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                communicator.promptForLogin();
              }
            });
          }
        });
      }
    });
    List<ModelingCommons.Group> groups = new ArrayList<ModelingCommons.Group>(communicator.getGroups());
    groups.add(0, null);
    groupComboBox.setModel(new DefaultComboBoxModel(groups.toArray()));
    everyonePermissionIndex = visibilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("a"), true);
    changeabilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("a"), true);
    groupPermissionIndex = visibilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("g"), false);
    changeabilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("g"), false);
    userPermissionIndex = visibilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("u"), true);
    changeabilityComboBox.addItem(ModelingCommons.Permission.getPermissions().get("u"), true);


    groupComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        boolean groupSelected = !(groupComboBox.getSelectedItem() == null);
        visibilityComboBox.setIndexEnabled(groupPermissionIndex, groupSelected);
        changeabilityComboBox.setIndexEnabled(groupPermissionIndex, groupSelected);

        ModelingCommons.Permission visibility = (ModelingCommons.Permission)(visibilityComboBox.getSelectedItem());
        if(!groupSelected && visibility.getId().equals("g")) {
          visibilityComboBox.setSelectedIndex(userPermissionIndex);
        }

        ModelingCommons.Permission changeability = (ModelingCommons.Permission)(changeabilityComboBox.getSelectedItem());
        if(!groupSelected && changeability.getId().equals("g")) {
          changeabilityComboBox.setSelectedIndex(userPermissionIndex);
        }



      }
    });

    selectFileButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        try {
          uploadImageFilePath = FileDialog.show(frame, "Select image to use as preview image", java.awt.FileDialog.LOAD);
          String toSet = uploadImageFilePath;
          FontMetrics metrics = selectedFileLabel.getFontMetrics(selectedFileLabel.getFont());
          System.out.println("" + selectedFileLabel.getMaximumSize().width);
          while(metrics.stringWidth(toSet) > selectedFileLabel.getMaximumSize().width) {
            toSet = "\u2026" + toSet.substring(2);
          }
          selectedFileLabel.setText(toSet);
        } catch(UserCancelException e) {}
      }
    });
    useCurrentViewRadioButton.setSelected(true);
    selectedFileLabel.setEnabled(false);
    selectFileButton.setEnabled(false);
    imageFromFileRadioButton.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        selectedFileLabel.setEnabled(imageFromFileRadioButton.isSelected());
        selectFileButton.setEnabled(imageFromFileRadioButton.isSelected());
      }
    });

// call onCancel() when cross is clicked
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        onCancel();
      }
    });

// call onCancel() on ESCAPE
    contentPane.registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onCancel();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
  }
  private boolean isValidInput() {
    if(modelNameField.getText().trim().length() == 0) {
      errorLabel.setText("Missing model name");
      return false;
    }
    return true;
  }
  private void onOK() {
    if(!isValidInput()) {
      return;
    }
    dispose();
    ModalProgressTask.apply(frame, "Uploading model to Modeling Commons", new Runnable() {
      public void run() {
        String modelName = modelNameField.getText().trim();
        ModelingCommons.Group group = (ModelingCommons.Group)groupComboBox.getSelectedItem();
        ModelingCommons.Permission visibility = (ModelingCommons.Permission)visibilityComboBox.getSelectedItem();
        ModelingCommons.Permission changeability = (ModelingCommons.Permission)changeabilityComboBox.getSelectedItem();
        ModelingCommons.PreviewImage previewImage = null;
        if(useCurrentViewRadioButton.isSelected()) {
          System.out.println("Preview image is current preview");
          previewImage = communicator.new CurrentViewPreviewImage();
        } else if(imageFromFileRadioButton.isSelected()) {
          if(uploadImageFilePath != null) {
            System.out.println("Preview image is path " + uploadImageFilePath);
            previewImage = communicator.new FilePreviewImage(uploadImageFilePath);
          }
        }


        final String result = communicator.uploadModel(
            modelName,
            group,
            visibility,
            changeability,
            previewImage
        );
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if(result.equals("NOT_LOGGED_IN")) {
              communicator.promptForLogin();
            } else if(result.equals("MISSING_PARAMETERS")) {
              communicator.promptForUpload("Missing model name");
            } else if(result.equals("MODEL_NOT_SAVED")) {
              communicator.promptForUpload("Server error");
            } else if(result.equals("CONNECTION_ERROR")) {
              communicator.promptForUpload("Error connecting to Modeling Commons");
            } else if(result.equals("SUCCESS")) {
              communicator.promptForSuccess();
            } else if(result.equals("INVALID_PREVIEW_IMAGE")) {
              communicator.promptForUpload("Invalid preview image");
            } else if(result.equals("SUCCESS_PREVIEW_NOT_SAVED")) {
              communicator.promptForSuccess("The model was uploaded, but the preview image was not saved");
            } else {
              communicator.promptForUpload("Unknown server error");
            }
          }
        });
      }
    });
  }

  private void onCancel() {
    dispose();
  }


}
