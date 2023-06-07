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
    public static String getRequiredArgumentValue(List<String> arguments, String argumentName) {
        String outputPath = getArgument(arguments, argumentName);
        if (outputPath == null) {
            System.err.println("Value not found for argument " + argumentName);
            System.exit(1);
        }
        return outputPath;
    }

    /**
     * Writes the execution result to a file
     */
    public static void writeExecutionResultToFile(Integer exitCode, String executionResultOutputPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(executionResultOutputPath))) {
            writer.write(String.format("%d", exitCode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isParamsFile(String argument) {
        return argument.startsWith("@");
    }

    /**
     * Read-in arguments from a params-file
     */
    public static List<String> readArgumentsFromFile(String filePath) {
        try {
            if (isParamsFile(filePath)) {
                filePath = filePath.substring(1);
            }
            Path path = Paths.get(filePath);
            return Files.readAllLines(path);
        } catch (IOException e) {
            System.err.println("An error occurred while reading the params file: " + e.getMessage());
            System.exit(1);
        }
        return null;
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
