package com.devicehive.shim.kafka.server;

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.Listener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientRequestHandler.class);

    private Listener listener;
    private ExecutorService requestExecutor;
    private Producer<String, Response> responseProducer;
    private Gson gson;

    public ClientRequestHandler(Listener listener, ExecutorService requestExecutor, Producer<String, Response> responseProducer) {
        this.listener = listener;
        this.requestExecutor = requestExecutor;
        this.responseProducer = responseProducer;

        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    void handleRequest(Request request) {
        final String replyTo = request.getReplyTo();
        assert replyTo != null;

        CompletableFuture.supplyAsync(() -> listener.onMessage(request), requestExecutor)
                .handleAsync((ok, ex) -> {
                    Response response;
                    if (ok != null) {
                         String body = gson.toJson(ok);
                         response = Response.newBuilder()
                                .withContentType(request.getContentType())
                                .withCorrelationId(request.getCorrelationId())
                                .withBody(body.getBytes(Charset.forName("UTF-8")))
                                .buildSuccess();
                    } else {
                        //todo better exception handling here
                        response = Response.newBuilder()
                                .withContentType(request.getContentType())
                                .withErrorCode(500)
                                .buildFailed();
                    }
                    sendReply(replyTo, request.getCorrelationId(), response);
                    return null;
                }, requestExecutor);

    }

    private void sendReply(String replyTo, String key, Response response) {
        responseProducer.send(new ProducerRecord<>(replyTo, key, response), (recordMetadata, e) -> {
            if (e != null) {
                logger.error("Send response failed", e);
            }
            logger.debug("Response {} sent successfully", key);
        });
    }

    public void shutdown() {
        if (requestExecutor != null) {
            requestExecutor.shutdown();
            try {
                requestExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Exception occurred while shutting executor service: {}", e);
            }
        }
        if (responseProducer != null) {
            responseProducer.close();
        }
    }
}
