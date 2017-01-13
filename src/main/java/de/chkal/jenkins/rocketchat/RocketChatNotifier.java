package de.chkal.jenkins.rocketchat;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;

import static java.lang.String.format;

@Extension
public class RocketChatNotifier extends RunListener<Run<?, ?>> implements Describable<RocketChatNotifier>, ExtensionPoint {

  @Override
  public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {

    if (!Boolean.TRUE.equals(getDescriptor().getEnableNotifications())) {
      return;
    }

    boolean useAllMentions = Boolean.TRUE.equals(getDescriptor().getUseAllMentions());
    NotificationManager notificationManager = new NotificationManager();

    Notification notification = notificationManager.createNotification(run);
    if (notification != null) {
      sendNotification(notification, useAllMentions, listener);
    }

  }

  private void sendNotification(Notification notification, boolean useAllMentions, TaskListener listener) {

    listener.getLogger().println(format("Sending notify: '%s'", notification.toString()));

    try {

      RocketChatClient client = getDescriptor().getRocketChatClient();
      client.send(notification, useAllMentions);

    } catch (IOException e) {
      listener.getLogger().println("Rocket.Chat notification failed: " + e.getMessage());
    }
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<RocketChatNotifier> {

    private Boolean enableNotifications;

    private String webhook;

    private Boolean useAllMentions;

    private transient RocketChatClient rocketChatClient;

    public DescriptorImpl() {
      load();
    }

    public FormValidation doCheckUrl(@QueryParameter String value) {
      if (value != null && !value.trim().isEmpty()) {
        if (!value.trim().startsWith("http")) {
          return FormValidation.error("This doesn't look like a correct URL");
        }
      }
      return FormValidation.ok();
    }

    public FormValidation doTestConnection(@QueryParameter("webhook") String webhook) {

      try {

        if (webhook == null || webhook.trim().isEmpty()) {
          return FormValidation.error("Webhook is missing");
        }

        Notification notification = new Notification();
        notification.setText("Ping!");

        RocketChatClient client = new RocketChatClient(webhook);
        client.send(notification, useAllMentions);

        return FormValidation.ok("Everything works fine!");

      } catch (Exception e) {
        return FormValidation.error(e.getMessage());
      }
    }

    public RocketChatClient getRocketChatClient() {
      if (rocketChatClient == null) {
        rocketChatClient = new RocketChatClient(webhook);
      }
      return rocketChatClient;
    }

    public String getDisplayName() {
      return "Rocket.Chat Notifier";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      req.bindJSON(this, formData);
      save();
      return super.configure(req, formData);
    }

    @Override
    public synchronized void load() {
      rocketChatClient = null;
      super.load();
    }

    @Override
    public synchronized void save() {
      rocketChatClient = null;
      super.save();
    }

    public String getWebhook() {
      return webhook;
    }

    public void setWebhook(String webhook) {
      this.webhook = webhook;
    }

    public Boolean getUseAllMentions() {
      return useAllMentions;
    }

    public void setUseAllMentions(Boolean useAllMentions) {
      this.useAllMentions = useAllMentions;
    }

    public Boolean getEnableNotifications() {
      return enableNotifications;
    }

    public void setEnableNotifications(Boolean enableNotifications) {
      this.enableNotifications = enableNotifications;
    }
  }

  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
  }

}
