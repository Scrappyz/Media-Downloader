package com.scrappyz.ytdlp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ProcessUtils {

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessResult {
        private List<String> output;
        private List<String> errorOutput;
        private int exitCode;

        public String getOutputAt(int index) {
            return output.get(index);
        }

        public String getErrorOutputAt(int index) {
            return errorOutput.get(index);
        }

        public boolean hasOutput() {
            return !output.isEmpty();
        }

        public boolean hasErrors() {
            return !errorOutput.isEmpty();
        }

        public boolean hasNoOutput() {
            return output.isEmpty() && errorOutput.isEmpty();
        }

        public int getOutputSize() {
            return output.size();
        }

        public int getErrorOutputSize() {
            return errorOutput.size();
        }
    }
    
    public static ProcessResult runProcess(List<String> command) throws IOException, InterruptedException {
        List<String> output = new ArrayList<>();
        List<String> errorOutput = new ArrayList<>();
        int exitCode = -1;

        ProcessBuilder pb = new ProcessBuilder(command);

        Process process = pb.start();

        Thread outputStreamConsumer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line); // Or handle the output line as needed
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        outputStreamConsumer.start();

        Thread errorStreamConsumer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.add(line); // Or handle the error line as needed
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        errorStreamConsumer.start();

        exitCode = process.waitFor();

        outputStreamConsumer.join();
        errorStreamConsumer.join();

        return new ProcessResult(output, errorOutput, exitCode);
    }
}
