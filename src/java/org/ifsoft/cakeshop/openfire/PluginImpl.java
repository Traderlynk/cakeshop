package org.ifsoft.cakeshop.openfire;

import java.io.File;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.nio.file.*;

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

import java.lang.reflect.*;
import java.util.*;

import org.jitsi.util.OSUtils;
import com.jpmorgan.cakeshop.config.*;


public class PluginImpl implements Plugin, PropertyEventListener, ProcessListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);

    private ServletContextHandler cakeshopContext;
    private ServletContextHandler apiContext;


    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        try {
            HttpBindManager.getInstance().removeJettyHandler(cakeshopContext);
            HttpBindManager.getInstance().removeJettyHandler(apiContext);
        }
        catch (Exception e) {
            //Log.error("IPFS destroyPlugin ", e);
        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);
        checkNatives(pluginDirectory);

        boolean cakeshopEnabled = JiveGlobals.getBooleanProperty("cakeshop.enabled", true);

        if (cakeshopEnabled)
        {
            addCakeshopProxy();

        } else {
            Log.info("cakeshop disabled");
        }
    }

    public String getIpAddress()
    {
        String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();
        String ourIpAddress = "127.0.0.1";

        try {
            ourIpAddress = InetAddress.getByName(ourHostname).getHostAddress();
        } catch (Exception e) {

        }

        return ourIpAddress;
    }



    private void addCakeshopProxy()
    {
        Log.info("Initialize CakeshopProxy");

        cakeshopContext = new ServletContextHandler(null, "/cakeshop", ServletContextHandler.SESSIONS);
        cakeshopContext.setClassLoader(this.getClass().getClassLoader());

        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet.setInitParameter("proxyTo", "http://127.0.0.1:8080/cakeshop");
        proxyServlet.setInitParameter("prefix", "/");
        cakeshopContext.addServlet(proxyServlet, "/*");

        HttpBindManager.getInstance().addJettyHandler(cakeshopContext);

        apiContext = new ServletContextHandler(null, "/api", ServletContextHandler.SESSIONS);
        apiContext.setClassLoader(this.getClass().getClassLoader());

        ServletHolder proxyServlet2 = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet2.setInitParameter("proxyTo", "http://127.0.0.1:8102");
        proxyServlet2.setInitParameter("prefix", "/");
        apiContext.addServlet(proxyServlet2, "/*");

        HttpBindManager.getInstance().addJettyHandler(apiContext);
    }

    private void checkNatives(File pluginDirectory)
    {
        try
        {
            String suffix = null;

            if(OSUtils.IS_LINUX32)
            {
                suffix = "linux-32";
            }
            else if(OSUtils.IS_LINUX64)
            {
                suffix = "linux-64";
            }
            else if(OSUtils.IS_WINDOWS64)
            {
                suffix = "win-64";
            }

            if (suffix != null)
            {

                Log.info("checkNatives cakeshop OS " + suffix);

            } else {

                Log.error("checkNatives unknown OS " + pluginDirectory.getAbsolutePath());
            }
        }
        catch (Exception e)
        {
            Log.error(e.getMessage(), e);
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
