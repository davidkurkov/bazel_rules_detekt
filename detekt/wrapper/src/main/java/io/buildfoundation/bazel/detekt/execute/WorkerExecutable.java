package io.buildfoundation.bazel.detekt.execute;

import io.buildfoundation.bazel.detekt.ExecutionUtils;
import io.buildfoundation.bazel.detekt.value.WorkRequest;
import io.buildfoundation.bazel.detekt.value.WorkResponse;

import java.util.List;

/**
 * WorkerExecutable interface representing a worker task.
 */
public interface WorkerExecutable {
    WorkResponse execute(WorkRequest request);

    final class Impl implements WorkerExecutable {
        private final Executable executable;

        public Impl(Executable executable) {
            this.executable = executable;
        }

        @Override
        public WorkResponse execute(WorkRequest request) {
            boolean shouldRunAsTestTarget = ExecutionUtils.shouldRunAsTestTarget(request.arguments);

            // Extract the output path of the execution result from the arguments
            String executionResultOutputPath = ExecutionUtils.getExecutionResultOutputPath(request.arguments);

            // Sanitize the Detekt arguments
            List<String> detektArgs = ExecutionUtils.sanitizeDetektArguments(request.arguments);

            // Execute detekt
            ExecutableResult executableResult = executable.execute(detektArgs.toArray(new String[0]));

            // Prepare the worker response
            WorkResponse workResponse = new WorkResponse();
            workResponse.requestId = request.requestId;
            workResponse.output = executableResult.output();
            workResponse.exitCode = executableResult.statusCode();

            // Write the execution result to a file
            ExecutionUtils.writeExecutionResultToFile(workResponse.exitCode, executionResultOutputPath);

            // Force the exit code to be 0 if detekt is run as a test target
            if (shouldRunAsTestTarget) {
                workResponse.exitCode = 0;
            }

            return workResponse;
        }
    }
}
