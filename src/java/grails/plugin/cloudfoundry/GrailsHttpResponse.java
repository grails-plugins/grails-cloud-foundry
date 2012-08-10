/* Copyright 2011-2012 SpringSource.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.cloudfoundry;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

/**
 * Based on package-default org.springframework.http.client.SimpleClientHttpResponse.
 *
 * @author Burt Beckwith
 */
public class GrailsHttpResponse implements ClientHttpResponse {

	private final HttpURLConnection connection;

	private HttpHeaders headers;

	/**
	 * Constructor.
	 * @param connection the connection
	 */
	public GrailsHttpResponse(HttpURLConnection connection) {
		this.connection = connection;
	}

	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(getRawStatusCode());
	}

	public int getRawStatusCode() throws IOException {
		return connection.getResponseCode();
	}

	public String getStatusText() throws IOException {
		return connection.getResponseMessage();
	}

	public HttpHeaders getHeaders() {
		if (headers == null) {
			headers = new HttpHeaders();
			// Header field 0 is the status line for most HttpURLConnections, but not on GAE
			String name = connection.getHeaderFieldKey(0);
			if (StringUtils.hasLength(name)) {
				headers.add(name, connection.getHeaderField(0));
			}
			int i = 1;
			while (true) {
				name = connection.getHeaderFieldKey(i);
				if (!StringUtils.hasLength(name)) {
					break;
				}
				headers.add(name, connection.getHeaderField(i));
				i++;
			}
		}
		return headers;
	}

	public InputStream getBody() throws IOException {
		InputStream errorStream = connection.getErrorStream();
		return (errorStream != null ? errorStream : connection.getInputStream());
	}

	public void close() {
		connection.disconnect();
	}
}
