package demo;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;

public class HystrixCommandWithErrorTest {
    private Logger logger = LoggerFactory.getLogger(HystrixCommandWithErrorTest.class);

    @Test
    public void error1Test() throws Exception {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        StringWithErrorCommand command1 = new StringWithErrorCommand("NewEchoHelloWorld1");
        String result = command1.execute();
        logger.info("Command called and the result is: {}", result);
    }


    @Test
    public void error2UsingRxTest() throws Exception {
        HystrixRequestContext context = HystrixRequestContext.initializeContext();
        StringWithErrorCommand command1 = new StringWithErrorCommand("HelloWorld2");
        Observable<StringWithErrorCommand> result = Observable.just(command1);
        result.subscribe(new Action1<StringWithErrorCommand>() {
            @Override
            public void call(StringWithErrorCommand s) {
                logger.info("Command called and the result is: {}", s.execute());
            }
        });
    }

    static class StringWithErrorCommand extends HystrixCommand<String> {
        public static final HystrixCommandKey COMMAND_KEY = HystrixCommandKey.Factory.asKey("StringWithErrorCommand");
        private static final Logger logger = LoggerFactory.getLogger(StringWithErrorCommand.class);

        private String input;

        protected StringWithErrorCommand(String input) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TestGroup"))
                            .andCommandKey(COMMAND_KEY)
                            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("TestThreadPool"))
                            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                    .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD))
                            .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                                    .withCoreSize(10))
            );
            this.input = input;
        }


        @Override
        protected String run() throws Exception {
            logger.info("Run command with input: {}", input);

            if (input != null) throw new OutOfMemoryError();

            return "It should not reach here! The message received was: " + input;
        }

        @Override
        protected String getCacheKey() {
            return input;
        }

        @Override
        protected String getFallback() {
            return new ErrorCommand(input, getFailedExecutionException()).execute();
        }
    }

    static class ErrorCommand extends HystrixCommand<String> {
        public static final HystrixCommandKey COMMAND_KEY = HystrixCommandKey.Factory.asKey("ErrorCommand");
        private final Throwable t;
        private static final Logger logger = LoggerFactory.getLogger(ErrorCommand.class);

        private String input;

        protected ErrorCommand(String input, Throwable t) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("TestGroup"))
                            .andCommandKey(COMMAND_KEY)
                            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey("TestThreadPool"))
                            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                                    .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD))
                            .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                                    .withCoreSize(10))
            );
            this.input = input;
            this.t = t;
        }

        @Override
        protected String run() throws Exception {
            logger.info("Run command with input: {} and the exception: {}", input, t);

            return "The message received: " + input +" and the exception/error: "+ t;
        }

        @Override
        protected String getCacheKey() {
            return input;
        }
    }
}
