package de.chkal.jenkins.rocketchat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.StringWriter;

class RocketChatClient {

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient client;

  private final JsonFactory jsonFactory;

  private final String webhook;

  RocketChatClient(String webhook) {

    this.client = new OkHttpClient();
    this.jsonFactory = new JsonFactory();

    this.webhook = webhook;
    if (this.webhook == null || this.webhook.trim().isEmpty()) {
      throw new IllegalStateException("No webhook URL provided!");
    }

  }

  void send(Notification notification, boolean useAllMentions) throws IOException {

    RequestBody body = RequestBody.create(JSON, toJson(notification, useAllMentions));

    Request request = new Request.Builder()
        .url(webhook)
        .post(body)
        .build();

    Response response = client.newCall(request).execute();

    response.body().close();

    if (!response.isSuccessful()) {
      throw new IOException(String.valueOf(response.code()));
    }

  }

  private String toJson(Notification notification, boolean useAllMentions) throws IOException {

    StringWriter writer = new StringWriter();
    JsonGenerator generator = jsonFactory.createGenerator(writer);

    generator.writeStartObject();

    StringBuilder text = new StringBuilder();
    if (useAllMentions) {
      text.append("@all ");
    }
    text.append(notification.getText());

    generator.writeStringField("text", text.toString());

    generator.writeArrayFieldStart("attachments");

    generator.writeStartObject();
    generator.writeStringField("title", notification.getRunName());
    generator.writeStringField("title_link", notification.getRunUrl());
    generator.writeStringField("text", notification.getText());
    generator.writeStringField("color", notification.getColor());
    generator.writeEndObject();

    generator.writeEndArray();
    generator.writeEndObject();

    generator.close();
    return writer.toString();

  }

}
