/*
* Copyright (C) 2024, 2024 Oracle and/or its affiliates.
*
* Licensed under the Universal Permissive License v 1.0 as shown at
* https://oss.oracle.com/licenses/upl/
*/

package oracle.nosql.intellij.plugin.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import oracle.nosql.intellij.plugin.common.ConnectionDataProviderService;
import oracle.nosql.intellij.plugin.common.MultipleConnectionsDataProviderService;
import oracle.nosql.intellij.plugin.common.MyHelpProvider;
import oracle.nosql.model.connection.ConfigurableProperty;
import oracle.nosql.model.connection.ConnectionFactory;
import oracle.nosql.model.connection.IConnectionProfileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class AddConnectionGUI {
    private ConnectionDataProviderService conService;
    private MultipleConnectionsDataProviderService mConService;
    private JPanel rootPanel;
    private JPanel subPanel;
    private JComboBox profileTypeComboBox;
    private String profileTypeName;
    private final IConnectionProfileType[] profileTypes = ConnectionFactory.getProfileTypes();
    private String connectionURL;
    private String connectionName;
    private String connectionCompartment;
    private String connectionNamespace;
    private String connectionTenantId;
    private Project project;
    private CloudPanelGUI cloudPanelGUI;

    AddConnectionGUI(JComponent component, Project project) {
        cloudPanelGUI = new CloudPanelGUI(project);
        profileTypeComboBox = (JComboBox) component;
        assert profileTypeComboBox != null;
        profileTypeComboBox.addItemListener(e -> {
            String name = (String) e.getItem();
            CardLayout cl = (CardLayout) subPanel.getLayout();
            cl.show(subPanel, name);
        });
        connectionURL = "";
        connectionName = "";
        connectionCompartment = "root";
        connectionNamespace = "sysdefault";
        connectionTenantId = "exampleId";

    }

    public JComponent createPanel(@NotNull MultipleConnectionsDataProviderService mConService) {
        conService = new ConnectionDataProviderService();
        this.mConService = mConService;
        if (profileTypes.length == 0) {
            throw new IllegalStateException("No registered profile types.");
        }
        String[] profileNames = new String[profileTypes.length];
        for (int i = 0; i < profileTypes.length; i++) {
            profileNames[i] = profileTypes[i].getName();
        }
        for (int i = 0; i < profileTypes.length; ++i) {
            if (!profileNames[i].equals("Cloud"))
                subPanel.add(profileNames[i], getProfileTypeSpecificUI(profileTypes[i]));
            else if (profileNames[i].equals("Cloud")) {
                cloudPanelGUI.setAdvancePanel(getProfileTypeSpecificUI(profileTypes[i]));
                subPanel.add(profileNames[i], cloudPanelGUI.getRootPanel());
            }

        }

        CardLayout cl = (CardLayout) subPanel.getLayout();
        cl.show(subPanel, Objects.requireNonNull(profileTypeComboBox.getSelectedItem()).toString());

        return rootPanel;
    }

    private JComponent getProfileTypeSpecificUI(IConnectionProfileType profileType) {
        int i = 3;
        JPanel panel = new JPanel();
        panel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:grow,left:4dlu:noGrow", // Column constraints
                "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow," + // Row constraints up to row 17
                        "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow," + "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow," + "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow," + "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow," + "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow," + // Row 18
                        "center:max(d;4px):noGrow" // Row 19
        ));
        CellConstraints cc = new CellConstraints();
        final Spacer spacer1 = new Spacer();
        panel.add(spacer1, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

        for (ConfigurableProperty property : profileType.getRequiredProperties()) {
            String prefKey = ConnectionDataProviderService.getKeyForProperty(profileType, property);
            JLabel propertyLabel = new JLabel();
            propertyLabel.setText(property.getLabel());
            panel.add(propertyLabel, cc.xy(1, i));

            if (property.getName().equals("SDK_PATH")) {
                TextFieldWithBrowseButton propertyText = new TextFieldWithBrowseButton();
                propertyText.putClientProperty("validator", property.getValidator());
                propertyText.putClientProperty("default", property.getDefaultValue());
                propertyText.putClientProperty("key", prefKey);
                propertyText.setToolTipText(property.getDescription());
                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
                String textVal = conService.getValue(prefKey);
                if (textVal == null) {
                    propertyText.setText("");
                    conService.putValue(prefKey, "");
                } else {
                    propertyText.setText(textVal);
                }
                //noinspection DialogTitleCapitalization
                propertyText.addBrowseFolderListener("", "SDK Path", null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
                panel.add(propertyText, cc.xyw(3, i, 2));
            } else if (property.getName().equals("PRIVATEKEY")) {
                TextFieldWithBrowseButton privatekey = new TextFieldWithBrowseButton();
                privatekey.putClientProperty("validator", property.getValidator());
                privatekey.putClientProperty("default", property.getDefaultValue());
                privatekey.putClientProperty("key", prefKey);
                privatekey.setToolTipText(property.getDescription());
                FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, false);
                String textVal = conService.getValue(prefKey);
                if (textVal == null) {
                    privatekey.setText("");
                    conService.putValue(prefKey, "");
                } else {
                    privatekey.setText(textVal);
                }
                privatekey.addBrowseFolderListener("", "Private key path", null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
                panel.add(privatekey, cc.xyw(3, i, 2));
            } else if (property.getName().equals("PASSPHRASE")) {
                JPasswordField passphrase = new JPasswordField();
                String textVal = conService.getValue(prefKey);
                if (textVal == null) {
                    passphrase.setText("");
                    conService.putValue(prefKey, "");
                } else {
                    passphrase.setText(textVal);
                }
                passphrase.putClientProperty("validator", null);
                passphrase.putClientProperty("default", property.getDefaultValue());
                passphrase.putClientProperty("key", prefKey);
                passphrase.setToolTipText(property.getDescription());
                panel.add(passphrase, cc.xy(3, i, CellConstraints.FILL, CellConstraints.CENTER));
            } else if (property.getName().equals("SECURITY")) {
                String[] dropdownList = {"SSL", "None"};
                ComboBox<String> secComBox = new ComboBox<>(dropdownList);
                JTextField username = new JTextField();
                JTextField password = new JPasswordField();
                TextFieldWithBrowseButton trustStore = new TextFieldWithBrowseButton();
                JTextField passphrase = new JPasswordField();
                JLabel usernameLabel = new JLabel();
                JLabel passwordLabel = new JLabel();
                JLabel trustStoreLabel = new JLabel();
                JLabel passphraseLabel = new JLabel();

                secComBox.putClientProperty("default", property.getDefaultValue());
                secComBox.putClientProperty("key", prefKey);
                secComBox.setToolTipText(property.getDescription());

                String comboxVal = conService.getValue(prefKey);
                if (comboxVal == null) {
                    secComBox.setSelectedItem(property.getDefaultValue());
                    conService.putValue(prefKey, property.getDefaultValue());
                } else {
                    secComBox.setSelectedItem(comboxVal);
                }
                panel.add(secComBox, cc.xyw(3, i, 1));
                secComBox.addItemListener(e -> {
                    if (Objects.equals(secComBox.getSelectedItem(), dropdownList[0])) {
                        username.setVisible(true);
                        password.setVisible(true);
                        trustStore.setVisible(true);
                        passphrase.setVisible(true);
                        usernameLabel.setVisible(true);
                        passwordLabel.setVisible(true);
                        trustStoreLabel.setVisible(true);
                        passphraseLabel.setVisible(true);
                        username.putClientProperty("validator", property.getValidator());
                        password.putClientProperty("validator", property.getValidator());
                        trustStore.putClientProperty("validator", property.getValidator());
                        passphrase.putClientProperty("validator", property.getValidator());
                    } else if (Objects.equals(secComBox.getSelectedItem(), dropdownList[1])) {
                        username.setText("");
                        password.setText("");
                        trustStore.setText("");
                        passphrase.setText("");
                        username.setVisible(false);
                        password.setVisible(false);
                        trustStore.setVisible(false);
                        passphrase.setVisible(false);
                        usernameLabel.setVisible(false);
                        passwordLabel.setVisible(false);
                        trustStoreLabel.setVisible(false);
                        passphraseLabel.setVisible(false);
                        username.putClientProperty("validator", null);
                        password.putClientProperty("validator", null);
                        trustStore.putClientProperty("validator", null);
                        passphrase.putClientProperty("validator", null);
                    }
                });
                for (ConfigurableProperty optProperty : profileType.getOptionalProperties()) {
                    switch (optProperty.getName()) {
                        case "USER_NAME": {
                            String userprefKey = ConnectionDataProviderService.getKeyForProperty(profileType, optProperty);
                            username.putClientProperty("default", optProperty.getDefaultValue());
                            username.putClientProperty("key", userprefKey);
                            username.setToolTipText(optProperty.getDescription());
                            panel.add(username, cc.xyw(3, i + 2, 1));
                            usernameLabel.setText(optProperty.getLabel());
                            panel.add(usernameLabel, cc.xy(1, i + 2));
                            String textVal = conService.getValue(userprefKey);
                            if (textVal == null) {
                                username.setText("");
                                conService.putValue(userprefKey, "");
                            } else {
                                username.setText(textVal);
                            }
                            if (Objects.equals(secComBox.getSelectedItem(), dropdownList[0])) {
                                username.setVisible(true);
                                usernameLabel.setVisible(true);
                                username.putClientProperty("validator", optProperty.getValidator());
                            } else {
                                username.setVisible(false);
                                username.setText("");
                                usernameLabel.setVisible(false);
                                username.putClientProperty("validator", null);
                            }
                            break;
                        }
                        case "PASSWORD": {
                            String passwprefKey = ConnectionDataProviderService.getKeyForProperty(profileType, optProperty);
                            passwordLabel.setText(optProperty.getLabel());
                            password.putClientProperty("default", optProperty.getDefaultValue());
                            password.putClientProperty("key", passwprefKey);
                            password.setToolTipText(optProperty.getDescription());
                            panel.add(password, cc.xyw(3, i + 4, 1));
                            panel.add(passwordLabel, cc.xy(1, i + 4));
                            String textVal = conService.getValue(passwprefKey);
                            if (textVal == null) {
                                password.setText("");
                                conService.putValue(passwprefKey, "");
                            } else {
                                password.setText(textVal);
                            }
                            if (Objects.equals(secComBox.getSelectedItem(), dropdownList[0])) {
                                password.setVisible(true);
                                passwordLabel.setVisible(true);
                                password.putClientProperty("validator", optProperty.getValidator());
                            } else {
                                password.putClientProperty("validator", null);
                                password.setText("");
                                password.setVisible(false);
                                passwordLabel.setVisible(false);
                            }
                            break;
                        }
                        case "TRUST_STORE": {
                            String trustStorePrefKey = ConnectionDataProviderService.getKeyForProperty(profileType, optProperty);
                            trustStoreLabel.setText(optProperty.getLabel());
                            panel.add(trustStoreLabel, cc.xy(1, i + 6));
                            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, false);

                            String textVal = conService.getValue(trustStorePrefKey);
                            if (textVal == null) {
                                trustStore.setText("");
                                conService.putValue(trustStorePrefKey, "");
                            } else {
                                trustStore.setText(textVal);
                            }
                            trustStore.putClientProperty("default", optProperty.getDefaultValue());
                            trustStore.putClientProperty("key", trustStorePrefKey);
                            trustStore.setToolTipText(optProperty.getDescription());
                            trustStore.addBrowseFolderListener("", "Trust store file", null, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
                            panel.add(trustStore, cc.xyw(3, i + 6, 1));

                            if (Objects.equals(secComBox.getSelectedItem(), dropdownList[0])) {
                                trustStore.setVisible(true);
                                trustStoreLabel.setVisible(true);
                                trustStore.putClientProperty("validator", optProperty.getValidator());
                            } else {
                                trustStore.putClientProperty("validator", null);
                                trustStore.setText("");
                                trustStore.setVisible(false);
                                trustStoreLabel.setVisible(false);
                            }
                            break;
                        }
                        case "TS_PASSPHRASE": {
                            String passphrasePrefKey = ConnectionDataProviderService.getKeyForProperty(profileType, optProperty);
                            passphraseLabel.setText(optProperty.getLabel());
                            passphrase.putClientProperty("default", optProperty.getDefaultValue());
                            passphrase.putClientProperty("key", passphrasePrefKey);
                            passphrase.setToolTipText(optProperty.getDescription());
                            panel.add(passphrase, cc.xyw(3, i + 8, 1));
                            panel.add(passphraseLabel, cc.xy(1, i + 8));
                            String textVal = conService.getValue(passphrasePrefKey);
                            if (textVal == null) {
                                passphrase.setText("");
                                conService.putValue(passphrasePrefKey, "");
                            } else {
                                passphrase.setText(textVal);

                            }
                            if (Objects.equals(secComBox.getSelectedItem(), dropdownList[0])) {
                                passphrase.setVisible(true);
                                passphraseLabel.setVisible(true);
                                passphrase.putClientProperty("validator", optProperty.getValidator());
                            } else {
                                passphrase.putClientProperty("validator", null);
                                passphrase.setText("");
                                passphrase.setVisible(false);
                                passphraseLabel.setVisible(false);
                            }
                            break;
                        }

                    }
                }
            } else {
                JTextField propertyText = new JTextField();
                String textVal = conService.getValue(prefKey);
                if (textVal == null) {
                    propertyText.setText("");
                    conService.putValue(prefKey, "");
                } else {
                    propertyText.setText(textVal);
                }
                propertyText.putClientProperty("validator", property.getValidator());
                if (property.getName().equals("COMPARTMENT")) {
                    propertyText.putClientProperty("validator", null);
                }
                propertyText.putClientProperty("default", property.getDefaultValue());
                propertyText.putClientProperty("key", prefKey);
                propertyText.setToolTipText(property.getDescription());
                panel.add(propertyText, cc.xy(3, i, CellConstraints.FILL, CellConstraints.CENTER));
            }
            i += 2;
        }
        return panel;
    }

    private JPanel getTopPanel() {
        JPanel panel = null;
        if (Objects.requireNonNull(profileTypeComboBox.getSelectedItem()).toString().equals("Cloud") && cloudPanelGUI.getSelectedIndex() == 1) {
            for (Component component : subPanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel p1 = (JPanel) component;
                    for (Component c1 : p1.getComponents()) {
                        if (c1 instanceof JTabbedPane) {
                            JTabbedPane t1 = (JTabbedPane) c1;
                            JScrollPane selectedComponent = (JScrollPane) t1.getSelectedComponent();
                            JViewport vp = selectedComponent.getViewport();
                            for (Component c2 : vp.getComponents()) {
                                if (c2 instanceof JPanel) {
                                    JPanel p2 = (JPanel) c2;
                                    for (Component ans : p2.getComponents()) {
                                        if (ans instanceof JPanel) {
                                            panel = (JPanel) ans;
                                            break;
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            return panel;
        } else {
            for (Component component : subPanel.getComponents()) {
                if (component.isVisible()) {
                    panel = (JPanel) component;
                    break;
                }
            }
            return panel;
        }
    }

    public ConnectionDataProviderService.State apply() throws ConfigurationException {
        if (Objects.requireNonNull(profileTypeComboBox.getSelectedItem()).toString().equals("Cloud") && cloudPanelGUI.getSelectedIndex() == 0) {
            cloudPanelGUI.apply();
            return null;
        }
        validate();
        Container parentComp = getTopPanel();
        for (Component component : parentComp.getComponents()) {
            if (component instanceof JTextField) {
                JTextField propertyText = (JTextField) component;
                if (propertyText.getClientProperty("ToolTipText").equals("Service URL") || propertyText.getClientProperty("ToolTipText").equals("Proxy URL") || propertyText.getClientProperty("ToolTipText").equals("Data Region endpoint"))
                    connectionURL = propertyText.getText();
                if (propertyText.getClientProperty("ToolTipText").equals("Connection Name"))
                    connectionName = propertyText.getText();
                conService.putValue(propertyText.getClientProperty("key").toString(), propertyText.getText());
            }
            if (component instanceof TextFieldWithBrowseButton) {
                TextFieldWithBrowseButton propertyText = (TextFieldWithBrowseButton) component;
                conService.putValue(propertyText.getClientProperty("key").toString(), propertyText.getText());
            }
            if (component instanceof ComboBox) {
                ComboBox secComBox = (ComboBox) component;
                conService.putValue(secComBox.getClientProperty("key").toString(), (String) secComBox.getSelectedItem());
            }
        }
        conService.putValue(ConnectionDataProviderService.KEY_PROFILE_TYPE, Objects.requireNonNull(profileTypeComboBox.getSelectedItem()).toString());
        String connectionUID = connectionURL;
        String comp = Objects.requireNonNull(conService.getState()).dict.get("/Cloud/COMPARTMENT");
        if (comp != null && !comp.isEmpty()) {
            connectionCompartment = new String(comp);
            connectionUID += " : " + connectionCompartment;
        }

        String namespace = Objects.requireNonNull(conService.getState()).dict.get("/Onprem/NAMESPACE");
        if (namespace != null && !namespace.isEmpty()) {
            connectionNamespace = new String(namespace);
            connectionUID += " : " + connectionNamespace ;
        }

        String tenant_id = Objects.requireNonNull(conService.getState()).dict.get("/Cloudsim/TENANT_ID");
        if(tenant_id!=null && !tenant_id.isEmpty()){
            connectionTenantId = new String(tenant_id);
            connectionUID += " : " + connectionTenantId;
        }

        profileTypeName = profileTypeComboBox.getSelectedItem().toString();
        Map<String, String> nameToUidMap = mConService.getNameToUidMap();
        if (!nameToUidMap.containsKey(connectionName)) mConService.putNameToUid(connectionName, connectionUID);
        else {
            String error = "Connection name already exists!";
            throw new ConfigurationException(error);
        }
        if(!mConService.getState().dict.containsKey(connectionUID)) {
            mConService.putValue(connectionUID, conService);
            mConService.putUidToType(connectionUID, profileTypeName);
        }
        else {
            String error = "Connection UID already exists!";
            throw new ConfigurationException(error);
        }
        return conService.getState();
    }

    @SuppressWarnings("unchecked")
    private void validate() throws ConfigurationException {
        Container parentComp = getTopPanel();
        for (Component component : parentComp.getComponents()) {
            if (component instanceof JTextField) {
                //String prefKey = (String) ((JTextField) component).getClientProperty("key");
                Function<String, String> validator;

                if ((validator = (Function<String, String>) ((JTextField) component).getClientProperty("validator")) != null) {
                    String error = validator.apply(((JTextField) component).getText());
                    if (error != null) {
                        throw new ConfigurationException(error);
                    }
                }
            } else if (component instanceof TextFieldWithBrowseButton) {
                //String prefKey = (String) ((TextFieldWithBrowseButton) component).getClientProperty("key");
                Function<String, String> validator;

                if ((validator = (Function<String, String>) ((TextFieldWithBrowseButton) component).getClientProperty("validator")) != null) {
                    String error = validator.apply(((TextFieldWithBrowseButton) component).getText());
                    if (error != null) {
                        throw new ConfigurationException(error);
                    }
                }
            }
        }
    }

    public String getConnectionNameAndUrl() {
        if (Objects.requireNonNull(profileTypeComboBox.getSelectedItem()).toString().equals("Cloud") && cloudPanelGUI.getSelectedIndex() == 0) {
            return cloudPanelGUI.getNameAndUrl();
        } else return connectionName + " : " + connectionURL;
    }
}