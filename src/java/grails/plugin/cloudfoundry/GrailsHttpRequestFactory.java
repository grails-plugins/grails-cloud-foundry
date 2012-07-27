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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.io.input.ProxyInputStream;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Extends SimpleClientHttpResponse to replace the HttpURLConnection's InputStream to one
 * that captures the response content.
 *
 * @author Burt Beckwith
 */
public class GrailsHttpRequestFactory extends SimpleClientHttpRequestFactory {

	private Proxy proxy;
	private static ByteArrayOutputStream bytes;

	/**
	 * Get the content from the last request. Not thread-safe.
	 * @return the content or null
	 */
	public static byte[] getLastResponse() {
		return bytes == null ? null : bytes.toByteArray();
	}

	/**
	 * Call before making a client call to reset any previous response.
	 */
	public static void resetResponse() {
		bytes = new ByteArrayOutputStream();
	}

	/**
	 * Sets the {@link Proxy} to use for this request factory.
	 * @param proxy the proxy
	 */
	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		URL url = uri.toURL();
		URLConnection urlConnection = proxy != null ? url.openConnection(proxy) : url.openConnection();
		Assert.isInstanceOf(HttpURLConnection.class, urlConnection);
		prepareConnection((HttpURLConnection)urlConnection, httpMethod.name());
		return new GrailsHttpRequest(wrap((HttpURLConnection)urlConnection));
	}

	protected HttpURLConnection wrap(final HttpURLConnection connection) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(HttpURLConnection.class);
		enhancer.setInterceptDuringConstruction(false);

		Callback callback = new MethodInterceptor() {
			@SuppressWarnings("hiding")
			public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
				try {
					Object value = method.invoke(connection, args);

					if ("getInputStream".equals(method.getName())) {
						return wrap((InputStream)value);
					}

					return value;
				}
				catch (InvocationTargetException ex) {
					throw ex.getCause();
				}
			}
		};
		enhancer.setCallbacks(new Callback[] { callback });
		enhancer.setCallbackTypes(new Class[] { callback.getClass() });

		return (HttpURLConnection)enhancer.create(
				new Class[] { URL.class },
				new Object[] { connection.getURL() });
	}

	protected InputStream wrap(final InputStream inputStream) {
		return new ProxyInputStream(inputStream) {
			@Override
			public int read(byte[] bts, int st, int end) throws IOException {
				int count = super.read(bts, st, end);
				if (count > 0) {
					bytes.write(bts, st, st + count);
				}
				return count;
			}
		};
	}
}
