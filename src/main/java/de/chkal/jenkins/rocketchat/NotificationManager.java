package de.chkal.jenkins.rocketchat;

import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.Jenkins;

class NotificationManager {

  Notification createNotification(Run<?, ?> run) {

    String notifyText = getNotifyText(run);

    if (notifyText != null) {

      String text = String.format("Build *%s*: %s", run.getFullDisplayName(), notifyText);

      String rootUrl = Jenkins.getInstance().getRootUrl();

      Notification notification = new Notification();
      notification.setRunName(run.getFullDisplayName());
      notification.setRunUrl(rootUrl + run.getUrl());
      notification.setText(text);
      notification.setColor(run.getIconColor().getHtmlBaseColor());
      return notification;

    }

    return null;

  }


  private String getNotifyText(Run run) {

    Result result = run.getResult();
    Result previous = run.getPreviousBuild() != null ? run.getPreviousBuild().getResult() : null;

    // compare result 
    if (previous != null) {

      if (isFailed(result) && result.equals(previous)) {
        return String.format("Status is still *%s*", result.toString());
      }

      if (isFailed(previous) && !isFailed(result)) {
        return String.format("Status is back to *%s*", result.toString());
      }

      if (!isFailed(previous) && isFailed(result)) {
        return String.format("Status is *%s*", result.toString());
      }

    }

    // no previous build
    else {
      if (isFailed(result)) {
        return String.format("Status is *%s*", result.toString());
      }
    }

    return null;

  }


  private boolean isFailed(Result result) {
    return result == Result.FAILURE
        || result == Result.NOT_BUILT
        || result == Result.UNSTABLE
        || result == Result.ABORTED;
  }


}
