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

import de.mxro.process.*;


public class PluginImpl implements Plugin, PropertyEventListener, ProcessListener
{
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);
    private String pluginDirectoryPath = null;
    private XProcess cakeshopThread = null;
    private String cakeshopExePath = null;
    private String cakeshopHomePath = null;
    private boolean cakeshopInitialise = false;
    private boolean cakeshopConfigure = false;
    private boolean cakeshopStart = false;
    private boolean cakeshopReady = false;
    private String cakeshopError = null;
    private ServletContextHandler cakeshopContext;
    private ExecutorService executor;
    private boolean keepRunning = true;

    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        try {
            keepRunning = false;

            if (executor != null)
            {
                executor.shutdown();
            }

            if (cakeshopThread != null) {
                cakeshopThread.destory();
            }

            Spawn.startProcess(cakeshopExePath + " shutdown", new File(cakeshopHomePath), this);

            HttpBindManager.getInstance().removeJettyHandler(cakeshopContext);
        }
        catch (Exception e) {
            //Log.error("CakeShop destroyPlugin ", e);
        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);
        pluginDirectoryPath = JiveGlobals.getProperty("cakeshop.path", JiveGlobals.getHomeDirectory() + File.separator + "cakeshop");
        checkNatives(pluginDirectory);

        boolean cakeshopEnabled = JiveGlobals.getBooleanProperty("cakeshop.enabled", true);

        if (cakeshopExePath != null && cakeshopEnabled)
        {
            executor = Executors.newCachedThreadPool();
            cakeshopThread = Spawn.startProcess(cakeshopExePath, new File(cakeshopHomePath), this);
            addCakeShopProxy();

        } else {
            Log.info("cakeshop disabled");
        }
    }

    public void sendLine(String command)
    {
        if (cakeshopThread != null) cakeshopThread.sendLine(command);
    }

    public String getPath()
    {
        return pluginDirectoryPath;
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

    public void onOutputLine(final String line) {
        Log.info(line);
    }

    public void onProcessQuit(int code) {
        Log.info("onProcessQuit " + code);

    }

    public void onOutputClosed() {
        Log.error("CakeShop terminated normally");
    }

    public void onErrorLine(final String line) {
        Log.error(line);
    }

    public void onError(final Throwable t) {
        Log.error("CakeShopThread error", t);
    }

    private void addCakeShopProxy()
    {
        Log.info("Initialize CakeShopProxy");

        cakeshopContext = new ServletContextHandler(null, "/cakeshop", ServletContextHandler.SESSIONS);
        //cakeshopContext.setClassLoader(this.getClass().getClassLoader());

        ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
        proxyServlet.setInitParameter("proxyTo", "http://" + getIpAddress() + ":8080/cakeshop");
        proxyServlet.setInitParameter("prefix", "/");
        cakeshopContext.addServlet(proxyServlet, "/*");

        HttpBindManager.getInstance().addJettyHandler(cakeshopContext);
    }

    private void checkNatives(File pluginDirectory)
    {
        File cakeshopFolder = new File(pluginDirectoryPath);

        if(!cakeshopFolder.exists())
        {
            Log.info("initializePlugin home " + pluginDirectory);
            cakeshopFolder.mkdirs();
        }

        try
        {
            String suffix = null;
            String warFile = null;

            if(OSUtils.IS_LINUX64)
            {
                suffix = "linux-64";
                warFile = "cakeshop-0.10.0-x86_64-linux.war";
            }
            else if(OSUtils.IS_WINDOWS64)
            {
                suffix = "win-64";
                warFile = "cakeshop-0.10.0-x86_64-windows.war";
            }

            if (suffix != null)
            {
                cakeshopHomePath = pluginDirectory.getAbsolutePath() + File.separator + "classes" + File.separator + suffix;
                cakeshopExePath = "java -Dgeth.node=geth -jar " + warFile;

                Log.info("checkNatives cakeshop executable path " + cakeshopExePath);

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
