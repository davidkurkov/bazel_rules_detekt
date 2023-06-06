package io.buildfoundation.bazel.detekt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExecutionUtils {
    /**
     * Returns true if run-as-test-target flag is included in arguments
     */
    public static boolean shouldRunAsTestTarget(List<String> arguments) {
        return arguments.contains("--run-as-test-target");
    }

    /**
     * Retrieves the output path for the test result from the input arguments.
     */
    public static String getExecutionResultOutputPath(List<String> inputArgs) {
        String outputPath = getArgument(inputArgs, "--execution-result");
        if (outputPath == null) {
            System.err.println("File path for execution-result was not set");
            System.exit(1);
        }
        return outputPath;
    }

    /**
     * Writes the execution result to a file
     */
    public static void writeExecutionResultToFile(Integer exitCode, String executionResultOutputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(executionResultOutputPath))) {
            writer.write(String.format("#!/bin/bash\n\nexit %d\n", exitCode));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in arguments from a params file
     */
    public static List<String> readArgumentsFromFile(String filePath) {
        // TODO: something
        try {
            Path path = Paths.get(filePath);
            return Files.readAllLines(path);
        } catch (IOException e) {
            System.err.println("An error occurred while reading the params file: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    /**
     * Write arguments to a params file
     */
    public static void writeArgumentsToFile(List<String> arguments, String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.write(path, arguments);
        } catch (IOException e) {
            System.err.println("An error occurred while writing arguments to params file: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sanitizes the Detekt arguments by excluding certain arguments.
     */
    public static List<String> sanitizeDetektArguments(List<String> inputArgs) {
        Set<String> excludedArgs = new HashSet<>(Arrays.asList("--execution-result", "--run-as-test-target"));
        return filterOutArgValuePairs(inputArgs, excludedArgs);
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
    public static String getArgument(List<String> inputArgs, String argName) {
        try {
            // Get the index of the argument and return the value at the next index.
            return inputArgs.get(inputArgs.indexOf(argName) + 1);
        } catch (IndexOutOfBoundsException ignored) {
            // Return null if the argument is not found or there's no value after it.
            return null;
        }
    }
}
