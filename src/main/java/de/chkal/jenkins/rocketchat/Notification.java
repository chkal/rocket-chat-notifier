package de.chkal.jenkins.rocketchat;

class Notification {

  private String runName;

  private String runUrl;

  private String text;

  private String color;

  public String getRunName() {
    return runName;
  }

  public void setRunName(String runName) {
    this.runName = runName;
  }

  public String getRunUrl() {
    return runUrl;
  }

  public void setRunUrl(String runUrl) {
    this.runUrl = runUrl;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }
}
