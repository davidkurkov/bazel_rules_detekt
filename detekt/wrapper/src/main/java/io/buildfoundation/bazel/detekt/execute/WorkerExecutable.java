package io.buildfoundation.bazel.detekt.execute;

import io.buildfoundation.bazel.detekt.value.WorkRequest;
import io.buildfoundation.bazel.detekt.value.WorkResponse;

import java.io.*;
import java.util.*;

public interface WorkerExecutable {

    WorkResponse execute(WorkRequest request);

    final class Impl implements WorkerExecutable {

        private final Executable executable;

        public Impl(Executable executable) {
            this.executable = executable;
        }

        @Override
        public WorkResponse execute(WorkRequest request) {
            boolean shouldRunAsTestTarget = request.arguments.contains("--run-as-test-target");
            String executionResultOutputPath = getExecutionResultOutputPath(request.arguments);

            List<String> detektArgs = sanitizeDetektArguments(request.arguments);

            ExecutableResult result = executable.execute(detektArgs.toArray(new String[0]));

            WorkResponse response = new WorkResponse();
            response.requestId = request.requestId;
            response.output = result.output();
            response.exitCode = result.statusCode();

            writeExecutionResultToFile(response.exitCode, executionResultOutputPath);

            if (shouldRunAsTestTarget) {
                response.exitCode = 0;
            }

            return response;
        }

        /**
         * Writes the execution result to a file
         */
        private static void writeExecutionResultToFile(Integer exitCode, String executionResultOutputPath) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(executionResultOutputPath))) {
                writer.write(String.format("#!/bin/bash\n\nexit %d\n", exitCode));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        private static List<String> sanitizeDetektArguments(List<String> inputArgs) {
            Set<String> excludeArgs = new HashSet<>(Arrays.asList("--execution-result", "--run-as-test-target"));
            return filterOutArgValuePairs(inputArgs, excludeArgs);
        }

        /**
         * Retrieves the output path for the test result from the input arguments.
         */
        private static String getExecutionResultOutputPath(List<String> inputArgs) {
            String outputPath = getArgument(inputArgs, "--execution-result");
            if (outputPath == null) {
                System.err.println("File path for execution-result was not set");
                System.exit(1);
            }
            return outputPath;
        }

        /**
         * Filters out specified arguments and their corresponding values from the input argument list.
         */
        public static List<String> filterOutArgValuePairs(List<String> args, Set<String> excludeArgs) {
            List<String> filteredList = new ArrayList<>();

            int index = 0;

            while (index < args.size()) {
                String value = args.get(index);
                if (!excludeArgs.contains(value)) {
                    filteredList.add(value);
                } else {
                    if (!args.get(index + 1).startsWith("--")) {
                        // Skip the arg-value pair since matching argument was found
                        index += 1;
                    }
                }
                index += 1;
            }

            return filteredList;
        }

        /**
         * Retrieves the value associated with the given argument name from the input arguments list.
         *
         * @param inputArgs List of input arguments.
         * @param argName The name of the argument whose value needs to be fetched.
         * @return The value associated with the given argument name or null if the argument is not found.
         */
        private static String getArgument(List<String> inputArgs, String argName) {
            try {
                // Get the index of the argument and return the value at the next index.
                return inputArgs.get(inputArgs.indexOf(argName) + 1);
            } catch (IndexOutOfBoundsException ignored) {
                // Return null if the argument is not found or there's no value after it.
                return null;
            }
        }
    }
}
