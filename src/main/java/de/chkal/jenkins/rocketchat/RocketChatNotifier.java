package de.chkal.jenkins.rocketchat;

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
import hudson.model.listeners.RunListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Set;

import static java.lang.String.format;

@Extension
public class RocketChatNotifier extends RunListener implements Describable<RocketChatNotifier>, ExtensionPoint {

  @Override
  public void onStarted(Run run, TaskListener listener) {
    // nothing to do here
  }

  @Override
  public void onCompleted(Run run, TaskListener listener) {

    String notifyText = getNotifyText(run);

    if (notifyText != null) {

      StringBuilder message = new StringBuilder();

      message.append(run.getFullDisplayName());
      message.append(": ");
      message.append(notifyText);
      message.append(" (");
      message.append(run.getBuildStatusSummary());
      message.append(")");

      chat(message.toString(), listener);

    }

  }

  private String getNotifyText(Run run) {

    Result result = run.getResult();
    Result previous = run.getPreviousBuild() != null ? run.getPreviousBuild().getResult() : null;

    // compare result 
    if (previous != null) {

      if (isFailed(result) && result.equals(previous)) {
        return "Build still " + result.toString();
      }

      if (isFailed(previous) && !isFailed(result)) {
        return "Build back to " + result.toString();
      }

      if (!isFailed(previous) && isFailed(result)) {
        return "Build status " + result.toString();
      }

    }

    // no previous build
    else {
      if (isFailed(result)) {
        return "Build status " + result.toString();
      }
    }

    return null;
    
  }

  private boolean isFailed(Result result) {
    return result == Result.FAILURE || result == Result.NOT_BUILT || result == Result.UNSTABLE;
  }

  private void chat(String message, TaskListener listener) {

    String room = getDescriptor().getRoom();

    listener.getLogger().println(
        format("Notifying %s/%s '%s'", getDescriptor().getUrl(), room, message)
    );

    try {

      RocketChatClient client = getDescriptor().getRocketChatClient();

      client.send(room, message);

    } catch (IOException e) {
      listener.getLogger().println("Rocket.Chat notification failed: " + e.getMessage());
      e.printStackTrace(listener.getLogger());
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
