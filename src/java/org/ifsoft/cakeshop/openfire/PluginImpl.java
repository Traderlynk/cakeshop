package org.ifsoft.cakeshop.openfire;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.servlets.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.webapp.WebAppContext;

import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;

import java.util.*;



public class PluginImpl implements Plugin, PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);
    private WebAppContext context4;


    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        try {
            HttpBindManager.getInstance().removeJettyHandler(context4);
        }
        catch (Exception e) {

        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);

        boolean cakeshopEnabled = JiveGlobals.getBooleanProperty("cakeshop.enabled", true);

        if (cakeshopEnabled)
        {
            Log.info("Initialize Cakeshop");

            String cakeshopHome = pluginDirectory.getPath() + File.separator + "classes" + File.separator + "data";
            System.setProperty("eth.config.dir", cakeshopHome);
            System.setProperty("geth.node", "geth");
            System.setProperty("cakeshop.spring.profiles.active", "local");
            System.setProperty("spring.config.location", "file:" + cakeshopHome +  File.separator + "data" + File.separator + "local" + File.separator + "application.properties");

            context4 = new WebAppContext(null, pluginDirectory.getPath() + "/classes/war", "/cakeshop");

            final List<ContainerInitializer> initializers4 = new ArrayList();
            initializers4.add(new ContainerInitializer(new JettyJasperInitializer(), null));
            context4.setAttribute("org.eclipse.jetty.containerInitializers", initializers4);
            context4.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

            HttpBindManager.getInstance().addJettyHandler(context4);

        } else {
            Log.info("cakeshop disabled");
        }
    }

//-------------------------------------------------------
//
//
//
//-------------------------------------------------------


    public void propertySet(String property, Map params)
    {

    }

    public void propertyDeleted(String property, Map<String, Object> params)
    {

    }

    public void xmlPropertySet(String property, Map<String, Object> params) {

    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {

    }

}
