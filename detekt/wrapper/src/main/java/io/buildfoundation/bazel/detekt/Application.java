package io.buildfoundation.bazel.detekt;

import io.buildfoundation.bazel.detekt.execute.Executable;
import io.buildfoundation.bazel.detekt.execute.ExecutableResult;
import io.buildfoundation.bazel.detekt.execute.WorkerExecutable;
import io.buildfoundation.bazel.detekt.stream.Streams;
import io.buildfoundation.bazel.detekt.stream.WorkerStreams;
import io.reactivex.rxjava3.core.Scheduler;

import java.util.Arrays;
import java.util.List;

public interface Application {

    void run(String[] args);

    final class OneShot implements Application {

        private final Executable executable;
        private final Streams streams;
        private final Platform platform;

        OneShot(Executable executable, Streams streams, Platform platform) {
            this.executable = executable;
            this.streams = streams;
            this.platform = platform;
        }

        @Override
        public void run(String[] args) {
            List<String> rawArgs = Arrays.asList(args);

            String paramsFilePath = (args.length == 1 && args[0].startsWith("@") && args[0].endsWith(".params")) ? args[0].substring(1) : "";
            if (!paramsFilePath.isEmpty()) {
                rawArgs = ExecutionUtils.readArgumentsFromFile(paramsFilePath);
            }

            boolean shouldRunAsTestTarget = ExecutionUtils.shouldRunAsTestTarget(rawArgs);

            // Extract the output path of the execution result from the arguments
            String executionResultOutputPath = ExecutionUtils.getExecutionResultOutputPath(rawArgs);

            // Sanitize the Detekt arguments
            List<String> detektArgs = ExecutionUtils.sanitizeDetektArguments(rawArgs);

            if (!paramsFilePath.isEmpty()) {
                // Write the sanitized arguments to the params-file
                ExecutionUtils.writeArgumentsToFile(detektArgs, paramsFilePath);
            }

            ExecutableResult result;
            if (!paramsFilePath.isEmpty()) {
                result = executable.execute(args);
            } else {
                result = executable.execute(detektArgs.toArray(new String[0]));
            }

            int statusCode = result.statusCode();

            if (result instanceof ExecutableResult.Failure) {
                streams.error().println(result.output());
            }

            // Write the execution result to a file
            ExecutionUtils.writeExecutionResultToFile(statusCode, executionResultOutputPath);

            // Force the exit code to be 0 if detekt is run as a test target
            if (shouldRunAsTestTarget) {
                statusCode = 0;
            }

            platform.exit(statusCode);
        }
    }

    final class Worker implements Application {

        private final WorkerExecutable executable;
        private final WorkerStreams streams;
        private final Scheduler scheduler;

        Worker(WorkerExecutable executable, WorkerStreams streams, Scheduler scheduler) {
            this.executable = executable;
            this.streams = streams;
            this.scheduler = scheduler;
        }

        @Override
        public void run(String[] args) {
            // something
            streams.request()
                .subscribeOn(scheduler)
                .parallel()
                .runOn(scheduler)
                .map(executable::execute)
                .sequential()
                .observeOn(scheduler)
                .blockingSubscribe(streams.response());
        }
    }
}
