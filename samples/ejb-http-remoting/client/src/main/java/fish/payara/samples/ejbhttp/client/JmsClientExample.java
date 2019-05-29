/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/master/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.samples.ejbhttp.client;

import com.sun.messaging.BasicQueue;
import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.QueueConnectionFactory;
import fish.payara.ejb.http.client.RemoteEJBContextFactory;
import fish.payara.ejb.http.client.adapter.ClientAdapter;
import fish.payara.ejb.http.client.adapter.ClientAdapterCustomizer;
import fish.payara.ejb.http.client.adapter.CompositeClientAdapter;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Optional;

import static fish.payara.ejb.http.client.adapter.ClientAdapterCustomizer.customize;
import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import static javax.naming.Context.PROVIDER_URL;

public enum JmsClientExample {
    INSTANCE;

    private final InitialContext context;

    JmsClientExample() {
        Hashtable<String, Object> environment = new Hashtable<>();
        environment.put(INITIAL_CONTEXT_FACTORY, "fish.payara.ejb.rest.client.RemoteEJBContextFactory");
        environment.put(PROVIDER_URL, "http://localhost:8080/ejb-invoker");

        QueueConnectionFactory jmsConnectionFactory = new QueueConnectionFactory();
        try {
            jmsConnectionFactory.setProperty(ConnectionConfiguration.imqAddressList,
                    "localhost:7676");
            CompositeClientAdapter adapter = CompositeClientAdapter.newBuilder().register(
                    customize((name, ctx) -> Optional.of(jmsConnectionFactory)).matchPrefix("jms/ConnectionFactory"),
                    customize(this::queueAdapter).matchPrefix("queue/")
                    ).build();
            environment.put(RemoteEJBContextFactory.CLIENT_ADAPTER, adapter);
            this.context = new InitialContext(environment);
        } catch (JMSException e) {
            throw new IllegalStateException("Cannot connect to JMS", e);
        } catch (NamingException e) {
            throw new IllegalStateException("Cannot construct naming context", e);
        }

    }

    private Optional<Object> queueAdapter(String name, Context remoteContext) throws NamingException {
        try {
            // that's the internal queue implementation that OpenMQ uses.
            return Optional.of(new BasicQueue(name));
        } catch (JMSException e) {
            NamingException namingException = new NamingException("Invalid queue name: " + name);
            namingException.setRootCause(namingException);
            throw namingException;
        }
    }

    public InitialContext getContext() {
        return context;
    }
}
