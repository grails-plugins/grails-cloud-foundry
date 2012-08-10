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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;

/**
 * Based on package-default org.springframework.http.client.SimpleClientHttpRequest.
 *
 * @author Burt Beckwith
 */
public class GrailsHttpRequest extends AbstractClientHttpRequest {

	private final HttpURLConnection connection;
	private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream();

	/**
	 * Constructor.
	 * @param connection the wrapped connection
	 */
	public GrailsHttpRequest(HttpURLConnection connection) {
		this.connection = connection;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(connection.getRequestMethod());
	}

	public URI getURI() {
		try {
			return connection.getURL().toURI();
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpURLConnection URI: " + ex.getMessage(), ex);
		}
	}

	protected ClientHttpResponse executeInternal(HttpHeaders headers, @SuppressWarnings("hiding") byte[] bufferedOutput) throws IOException {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				connection.addRequestProperty(headerName, headerValue);
			}
		}

		if (connection.getDoOutput()) {
			connection.setFixedLengthStreamingMode(bufferedOutput.length);
		}

		connection.connect();

		if (connection.getDoOutput()) {
			FileCopyUtils.copy(bufferedOutput, connection.getOutputStream());
		}

		return new GrailsHttpResponse(connection);
	}

	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		byte[] bytes = bufferedOutput.toByteArray();
		if (headers.getContentLength() == -1) {
			headers.setContentLength(bytes.length);
		}
		ClientHttpResponse result = executeInternal(headers, bytes);
		bufferedOutput = null;
		return result;
	}

	protected OutputStream getBodyInternal(HttpHeaders headers) {
		return bufferedOutput;
	}
}
