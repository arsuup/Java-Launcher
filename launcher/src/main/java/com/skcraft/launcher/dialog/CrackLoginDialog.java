package com.skcraft.launcher.dialog;

import com.skcraft.concurrency.ObservableFuture;
import com.skcraft.concurrency.ProgressObservable;
import com.skcraft.launcher.Launcher;
import com.skcraft.launcher.auth.OfflineSession;
import com.skcraft.launcher.auth.Session;
import com.skcraft.launcher.swing.*;
import com.skcraft.launcher.util.SharedLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;

public class CrackLoginDialog extends JDialog {

    private final Launcher launcher;
    @Getter private Session session;
    private final JTextField usernameText = new JTextField();

    public CrackLoginDialog(Window owner, @NonNull Launcher launcher, String initialUsername) {
        super(owner, ModalityType.DOCUMENT_MODAL);
        this.launcher = launcher;

        setTitle(SharedLocale.tr("login.title"));
        setLayout(new BorderLayout());

        FormPanel form = new FormPanel();
        form.addRow(new JLabel(SharedLocale.tr("login.username")), usernameText);
        usernameText.setText(initialUsername);

        JButton loginButton = new JButton(SharedLocale.tr("login.login"));
        JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));

        LinedBoxPanel buttons = new LinedBoxPanel(true);
        buttons.addGlue();
        buttons.addElement(loginButton);
        buttons.addElement(cancelButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> {
            String user = usernameText.getText().trim();
            if (!user.isEmpty()) {
                attemptLogin(user);
            }
        });
        cancelButton.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(loginButton);
        setMinimumSize(new Dimension(350, 150));
        pack();
        setLocationRelativeTo(owner);
    }

    private void attemptLogin(String username) {
        LoginCallable callable = new LoginCallable(username);
        ObservableFuture<Session> future = new ObservableFuture<>(
                launcher.getExecutor().submit(callable), callable);

        ProgressDialog.showProgress(this, future, SharedLocale.tr("login.loggingInTitle"), "");

        try {
            this.session = future.get();
            dispose();
        } catch (Exception e) {
            SwingHelper.showErrorDialog(this, "Erreur de login", "Erreur");
        }
    }

    public static Session showLoginRequest(Window owner, Launcher launcher, String username) {
        CrackLoginDialog dialog = new CrackLoginDialog(owner, launcher, username);
        dialog.setVisible(true);
        return dialog.getSession();
    }

    @RequiredArgsConstructor
    private static class LoginCallable implements Callable<Session>, ProgressObservable {
        private final String username;

        @Override
        public Session call() {
            return new OfflineSession(username);
        }

        @Override public double getProgress() { return -1; }
        @Override public String getStatus() { return ""; }
    }
}