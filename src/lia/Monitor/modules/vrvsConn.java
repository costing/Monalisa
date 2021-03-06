package lia.Monitor.modules;

import java.io.BufferedReader;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import lia.Monitor.monitor.AppConfig;
import lia.Monitor.monitor.MNode;
import lia.Monitor.monitor.MonModuleInfo;
import lia.Monitor.monitor.MonitoringModule;
import lia.Monitor.monitor.Result;
import lia.Monitor.monitor.cmdExec;
import lia.util.ntp.NTPDate;

public class vrvsConn extends cmdExec implements MonitoringModule {
    /**
     * 
     */
    private static final long serialVersionUID = -2664905244494465045L;

    /** The Logger */
    private static final Logger logger = Logger.getLogger(vrvsConn.class.getName());

    static String[] tmetric = { "Quality", "LostPackages" };

    String cmd;
    String Args;
    String base;
    String xarg = " vrvs/check_peer_status/30 ";
    Hashtable peers;

    public vrvsConn() {
        super("vrvsConn");
        info.ResTypes = tmetric;
        peers = new Hashtable();
        isRepetitive = true;
    }

    @Override
    public MonModuleInfo init(MNode Node, String arg) {
        this.Node = Node;
        info.ResTypes = tmetric;
        base = AppConfig.getProperty("MonaLisa.VRVS_HOME", "../..");
        cmd = base + "/bin/vrvs_cmd_LE -m " + xarg;

        try {
            BufferedReader buff2 = procOutput(cmd);
            buff2.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, " Failed  to get perform the init cmd");
        }

        return info;
    }

    @Override
    public Object doProcess() throws Exception {
        BufferedReader buff1 = procOutput(cmd);

        if (buff1 == null) {
            cleanup();
            logger.log(Level.WARNING, " Failed  to get the the vrvsConn output ");
            throw new Exception(" vrvsConn output  is null for " + Node.name);
        }

        Vector vec = null;
        try {
            vec = Parse(buff1);
        } catch (Throwable t) {
            cleanup();
            throw new Exception(t);
        }
        return vec;
    }

    public Vector Parse(BufferedReader buff) throws Exception {

        String lin = null;

        // Read until buffer emty or the PEER keyword found
        for (lin = buff.readLine(); (lin != null) && (lin.indexOf("PEER") == -1); lin = buff.readLine()) {
            ;
        }

        if (lin == null) {
            return null;
        }

        Result rr = null;
        Vector results = new Vector();

        for (lin = buff.readLine(); lin != null; lin = buff.readLine()) {

            if (lin.length() > 10) {
                StringTokenizer tz = new StringTokenizer(lin);
                String peerNameIP = tz.nextToken();

                String peerName = null;

                if (!peers.containsKey(peerNameIP)) {
                    try {
                        peerName = InetAddress.getByName(peerNameIP).getHostName();
                    } catch (Exception e) {
                    }
                    if (peerName == null) {
                        logger.log(Level.WARNING, "Failed to get Addrees for " + peerNameIP);
                        peers.put(peerNameIP, peerNameIP);

                    } else {
                        peers.put(peerNameIP, peerName);
                    }
                }

                peerName = (String) peers.get(peerNameIP);

                if (tz.hasMoreTokens()) {
                    rr = new Result(Node.getFarmName(), Node.getClusterName(), peerName, "vrvsConn", tmetric);

                    String v1 = tz.nextToken().trim();
                    String vv1 = v1.substring(0, v1.length() - 1);
                    double lo = (Double.valueOf(vv1)).doubleValue();
                    rr.param[0] = 100 - lo;
                    rr.param[1] = lo;
                    rr.time = NTPDate.currentTimeMillis();

                    results.add(rr);
                }
            }
        }

        buff.close();
        return results;
    }

    @Override
    public MonModuleInfo getInfo() {
        return info;
    }

    @Override
    public String[] ResTypes() {
        return tmetric;
    }

    @Override
    public String getOsName() {
        return "linux";
    }

    static public void main(String[] args) {

        vrvsConn aa = new vrvsConn();
        String ad = null;
        String host = "localhost";
        try {
            ad = InetAddress.getByName(host).getHostAddress();
        } catch (Exception e) {
            logger.log(Level.WARNING, " Can not get ip for node ", e);
            System.exit(-1);
        }

        MonModuleInfo info = aa.init(new MNode(host, ad, null, null), null, null);

        try {

            Object bb = aa.doProcess();

        } catch (Exception e) {
            System.out.println(" failed to process !!!");
        }

    }

}
