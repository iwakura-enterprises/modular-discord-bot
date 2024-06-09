package dev.mayuna.modularbot.utils;

import dev.mayuna.modularbot.logging.Logger;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.Request;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.requests.RestRateLimiter;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.requests.Requester;
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig;
import net.dv8tion.jda.internal.utils.config.SessionConfig;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

public class GlobalRateLimiter {

    private final @Getter Timer resetTimer = new Timer();
    private final Object mutex = new Object();
    private @Getter int requests;
    private @Getter int totalRequests;

    public GlobalRateLimiter() {
        resetTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requests = 0;
                synchronized (mutex) {
                    mutex.notifyAll();
                }
            }
        }, 0, ModularBotConfig.getInstance().getBot().getGlobalRateLimiter().getResetRequestsCountAfter());
    }

    public void processJda(JDA jda) {
        if (jda instanceof JDAImpl jdaImpl) {
            hijackIntoRequester(jdaImpl);
        }
    }

    public boolean isGloballyRateLimited() {
        return requests >= ModularBotConfig.getInstance().getBot().getGlobalRateLimiter().getMaxRequestCount();
    }

    /**
     * It creates a proxy of the Requester class, which is used by JDA to send requests to Discord. The proxy is used to intercept the #execute()
     * method, which is called every time a request is sent to Discord. The proxy then checks if the request is globally rate limited and if it is, it
     * waits until it's not
     *
     * @param jdaImpl The JDAImpl instance of the shard.
     */
    protected void hijackIntoRequester(JDAImpl jdaImpl) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(Requester.class);
        proxyFactory.setFilter(method -> method.getName().equals("request"));

        MethodHandler methodHandler = (self, thisMethod, proceed, args) -> {
            try {
                boolean addToRequests = true;
                String endpoint = null;

                if (args.length >= 1) { // There cannot be more arguments since the Requester#request() method has only one argument
                    if (args[0] instanceof Request<?> request) {
                        endpoint = request.getRoute().getBaseRoute().toString();
                        String[] ignoredEndpoints = ModularBotConfig.getInstance().getBot().getGlobalRateLimiter().getIgnoredEndpoints();

                        for (String ignoredEndpoint : ignoredEndpoints) {
                            // TODO: Tyto replacementy předělat do Configu #postLoadProcess(), ať se to neustále nedělá při každém requestu
                            String finalIgnoredEndpoint = ignoredEndpoint.replace(".", "_")
                                                                         .replace("@original", "{message_id}");

                            if (endpoint.contains(finalIgnoredEndpoint)) {
                                addToRequests = false;
                                Logger.flow("[GLOBAL RATE LIMITER] Endpoint '" + endpoint + "' is ignored (will not be globally rate limited)");
                                break;
                            }
                        }
                    }
                }

                if (addToRequests) {
                    while (isGloballyRateLimited()) {
                        synchronized (mutex) {
                            Logger.warn("[GLOBAL RATE LIMITER] Globally rate limited! Waiting...");
                            mutex.wait();
                        }
                    }

                    Logger.flow("[GLOBAL RATE LIMITER] Requesting endpoint " + endpoint);
                    requests++;
                    totalRequests++;
                }
            } catch (Exception exception) {
                Logger.get().error("Exception occurred while handling proxied Requester method call #request()!", exception);
            }

            return proceed.invoke(self, args);
        };

        try {
            RestConfig restConfig = (RestConfig) getObjectFromField(jdaImpl, "restConfig");
            ThreadingConfig threadConfig = (ThreadingConfig) getObjectFromField(jdaImpl, "threadConfig");
            SessionConfig sessionConfig = (SessionConfig) getObjectFromField(jdaImpl, "sessionConfig");

            boolean isRelative = sessionConfig.isRelativeRateLimit() && restConfig.isRelativeRateLimit();

            // FROM JDA SOURCE CODE (https://github.com/discord-jda/JDA)
            // JDA IS UNDER Apache-2.0 license
            /*
            RestRateLimiter recreatedRateLimiter = restConfig.getRateLimiterFactory().apply(new RestRateLimiter.RateLimitConfig(
                    threadConfig.getRateLimitPool(),
                    jdaImpl.getSessionController().getRateLimitHandle(),
                    isRelative
            ));

            Requester proxiedRequester = (Requester) proxyFactory.create(new Class<?>[]{JDA.class,
                                                                                        AuthorizationConfig.class,
                                                                                        RestConfig.class,
                                                                                        RestRateLimiter.class
            }, new Object[]{
                    jdaImpl,
                    jdaImpl.getAuthorizationConfig(),
                    restConfig,
                    recreatedRateLimiter
            }, methodHandler);

            Field field = JDAImpl.class.getDeclaredField("requester");
            field.setAccessible(true);
            field.set(jdaImpl, proxiedRequester);*/

            Logger.info("Hijacked Shard ID " + jdaImpl.getShardInfo().getShardId() + " with proxied Requester.");
        } catch (Exception exception) {
            Logger.get().error("Failed to hijack Shard ID " + jdaImpl.getShardInfo()
                                                                     .getShardId() + " with proxied Requester! GlobalRateLimiter won't work.", exception);
        }
    }

    private Object getObjectFromField(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }
}
