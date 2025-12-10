/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class NetworkUtil {

    private NetworkUtil() {
    }

    public static String session(Channel channel) {
        char transport = channel instanceof DatagramChannel ? 'U' : 'T';
        return transport + channel.id().asShortText();
    }

    public static String pingHost(String host) throws IOException {
        String command = "ping -c 3 " + host;
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    public static String runDiagnostic(String tool, String target) throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", "-c", tool + " " + target);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

}
