package com.acs.wave.examples.netty;

import com.acs.wave.converter.json.JsonBodyReader;
import com.acs.wave.converter.json.JsonBodyWriter;
import com.acs.wave.converter.json.json.ObjectMapperProvider;
import com.acs.wave.provider.netty.NettyServer;
import com.acs.wave.provider.netty.NettyServerBuilder;
import com.acs.wave.router.HTTPRouter;
import com.acs.wave.router.HTTPRouterBuilder;
import com.acs.wave.router.WebSocketRouter;
import com.acs.wave.router.WebSocketRouterBuilder;
import com.acs.wave.router.constants.ResponseStatus;
import com.acs.wave.router.files.StaticClasspathFolderFilter;
import com.acs.wave.router.websocket.request.WebSocketRequestText;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class NettyBoot {

    public static void main(String[] args) throws Exception {
        NettyServer nettyServer = new NettyServerBuilder()
                .webSocketRouter(getWebSocketRouter())
                .httpRouter(getHttpRouter())
                .build();

        nettyServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(nettyServer::stop));

    }

    private static WebSocketRouter getWebSocketRouter() {
        WebSocketRouterBuilder webSocketRouterBuilder = new WebSocketRouterBuilder();


//        webSocketRouterBuilder.webSocket("/revert", (request, responseBuilder) -> {
//            if (request instanceof WebSocketRequestText) {
//                return responseBuilder.text(revert(((WebSocketRequestText) request).text)).build();
//            } else {
//                return responseBuilder.build();
//            }
//        });

        webSocketRouterBuilder.webSocket("/echo", (request, responseBuilder) -> {
            if (request instanceof WebSocketRequestText) {
                return responseBuilder.text(((WebSocketRequestText) request).text).build();
            } else {
                return responseBuilder.build();
            }
        });

        return webSocketRouterBuilder.build();
    }

//    private static String revert(String text) {
//        String result = "";
//
//        for (int i = 0; i < text.length(); i++) {
//            result += text.charAt(text.length() - i - 1);
//        }
//
//        return result;
//    }

    private static HTTPRouter getHttpRouter() throws FileNotFoundException {
        Map<Long, Person> persons = new HashMap<>();
        AtomicLong ids = new AtomicLong(0);

        for (long i = 0; i < 10; i++) {
            Long id = ids.addAndGet(1);
            persons.put(id, new Person(id, "John Doe " + id, (int) (2 * id)));
        }

        HTTPRouterBuilder builder = new HTTPRouterBuilder();
        ObjectMapper objectMapper = new ObjectMapperProvider(true).getObjectMapper();
        JsonBodyWriter jsonBodyWriter = new JsonBodyWriter(objectMapper);
        JsonBodyReader<Person> jsonBodyReader = new JsonBodyReader<>(objectMapper, Person.class);

        builder.get("/persons", (request, responseBuilder) -> {
            responseBuilder.body(persons.values(), jsonBodyWriter);
            return Optional.of(responseBuilder.build());
        });

        builder.post("/persons", (request, responseBuilder) -> {
            Person person = request.body(jsonBodyReader);

            person = new Person(ids.addAndGet(1), person.getName(), person.getAge());
            persons.put(person.getId(), person);
            responseBuilder.body(person.getId(), jsonBodyWriter);
            responseBuilder.status(ResponseStatus.CREATED);
            return Optional.of(responseBuilder.build());
        });

        builder.put("/persons/{id}", (request, responseBuilder) -> {
            Person person = persons.get(request.pathParams().getMandatory("id", Long.class));
            Person personNew = request.body(jsonBodyReader);

            if (person != null) {
                personNew = new Person(person.getId(), personNew.getName(), personNew.getAge());
                persons.put(personNew.getId(), personNew);
                responseBuilder.body(personNew.getId(), jsonBodyWriter);
                return Optional.of(responseBuilder.build());
            } else {
                return Optional.of(responseBuilder.error(ResponseStatus.NOT_FOUND));
            }
        });

        builder.get("/persons/{id}", (request, responseBuilder) -> {
            Person person = persons.get(request.pathParams().getMandatory("id", Long.class));

            if (person != null) {
                responseBuilder.body(person, jsonBodyWriter);
                return Optional.of(responseBuilder.build());
            } else {
                return Optional.of(responseBuilder.error(ResponseStatus.NOT_FOUND));
            }
        });

        builder.delete("/persons/{id}", (request, responseBuilder) -> {
            Long id = request.pathParams().getMandatory("id", Long.class);

            if (persons.containsKey(id)) {
                persons.remove(id);
                return Optional.of(responseBuilder.build());
            } else {
                return Optional.of(responseBuilder.error(ResponseStatus.NOT_FOUND));
            }
        });

        builder.filter("/*", new StaticClasspathFolderFilter("public", true));
        builder.filter("/webjars/{path+}", new StaticClasspathFolderFilter("META-INF/resources/webjars", true));
        builder.handler("/", (request, responseBuilder) -> responseBuilder.serveOption("/index.html"));
        return builder.build();
    }
}
