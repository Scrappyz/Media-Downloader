package com.scrappyz.ytdlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YtdlpApplication {

	public static void main(String[] args) {
		SpringApplication.run(YtdlpApplication.class, args);
	}

}
// Fix bug where download finishes but is not being sent to the frontend. Perhaps frontend is the problem or the last endpoint.