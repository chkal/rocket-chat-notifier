package de.chkal.jenkins.rocketchat;

import static java.lang.String.format;

import java.io.IOException;
import java.util.Set;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.github.baloise.rocketchatrestclient.RocketChatClient;
import com.github.baloise.rocketchatrestclient.model.Room;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.View;
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

@Extension
public class RocketChatNotifier extends RunListener<Run<?, ?>> implements Describable<RocketChatNotifier>, ExtensionPoint {

  @Override
  public void onStarted(Run<?, ?> r, TaskListener listener) {
    notify(r, listener);
  }

  @Override
  public void onCompleted(Run<?, ?> r, TaskListener listener) {
    notify(r, listener);
  }

  public void notify(Run<?, ?> run, TaskListener listener) {
    String resultMessage = run.isBuilding() ? "STARTED" : run.getResult().toString();
    String message = format("%s, %s, %s, %s", run.getParent().getDisplayName(), run.getDisplayName(), resultMessage, run.getBuildStatusSummary().message);
    chat(message, listener);
  }

  private void chat(String message) {
    chat(message, null);
  }

  private void chat(final String message, final TaskListener listener) {
    if (listener != null)
      listener.getLogger().println(format("Notifying %s/%s '%s'", getDescriptor().getUrl(), getDescriptor().getRoom(), message));
    try {
      getDescriptor().getRocketChatClient().send(getDescriptor().getRoom(), message);
    } catch (IOException e) {
      e.printStackTrace();
      if (listener != null) listener.getLogger().println("Rocket.Chat Notification failed");
    }
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<RocketChatNotifier> {

    public DescriptorImpl() {
      load();
    }

    private String url;
    private String user;
    private String password;
    private String room;

    private transient RocketChatClient lazyRcClient;

    public FormValidation doCheckUrl(@QueryParameter String value) {
      return empty(value);
    }

    public FormValidation doCheckUser(@QueryParameter String value) {
      return empty(value);
    }

    public FormValidation doCheckPassword(@QueryParameter String value) {
      return empty(value);
    }

    public FormValidation doCheckRoom(@QueryParameter String value) {
      return empty(value);
    }

    public FormValidation doTestConnection(
        @QueryParameter("url") final String url,
        @QueryParameter("user") final String user,
        @QueryParameter("password") final String password,
        @QueryParameter("room") final String room
    ) {
      try {
        RocketChatClient rcClient = new RocketChatClient(url, user, password);
        Set<Room> publicRooms = rcClient.getPublicRooms();
        StringBuilder message = new StringBuilder("available rooms are: ");
        boolean comma = false;
        for (Room r : publicRooms) {
          if (r.name.equals(room))
            return FormValidation.ok("Server version is " + rcClient.getRocketChatVersion());
          if (comma) message.append(", ");
          comma = true;
          message.append("'" + r.name + "'");
        }
        return FormValidation.error("available rooms are " + message);
      } catch (Exception e) {
        return FormValidation.error(e.getMessage());
      }
    }

    public RocketChatClient getRocketChatClient() {
      if (lazyRcClient == null) {
        lazyRcClient = new RocketChatClient(url, user, password);
      }
      return lazyRcClient;
    }

    private FormValidation empty(String value) {
      return (value == null || value.isEmpty()) ? FormValidation.error("Must not be empty") : FormValidation.ok();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
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
      lazyRcClient = null;
      super.load();
    }

    @Override
    public synchronized void save() {
      lazyRcClient = null;
      super.save();
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getRoom() {
      return room;
    }

    public void setRoom(String room) {
      this.room = room;
    }

  }

  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
  }
}
