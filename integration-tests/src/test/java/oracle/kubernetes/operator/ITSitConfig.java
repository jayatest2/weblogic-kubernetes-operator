// Copyright 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.
package oracle.kubernetes.operator;

import static oracle.kubernetes.operator.BaseTest.initialize;
import static oracle.kubernetes.operator.BaseTest.logger;

import java.util.*;
import java.util.logging.Level;
import oracle.kubernetes.operator.utils.Domain;
import oracle.kubernetes.operator.utils.ExecCommand;
import oracle.kubernetes.operator.utils.ExecResult;
import oracle.kubernetes.operator.utils.Operator;
import oracle.kubernetes.operator.utils.TestUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

public class ITSitConfig extends BaseTest {

  private static String TESTSCRIPTDIR = "";

  private static String ADMINPODNAME = "";
  private static final String DOMAINUID = "customsitconfigdomain";
  private static final String ADMINPORT = "30710";
  private static final int T3CHANNELPORT = 30091;
  private static String fqdn = "";

  private static Domain domain = null;
  private static Operator operator1;
  /**
   * This method gets called only once before any of the test methods are executed. It does the
   * initialization of the integration test properties defined in OperatorIT.properties and setting
   * the resultRoot, pvRoot and projectRoot attributes.
   *
   * @throws Exception
   */
  @BeforeClass
  public static void staticPrepare() throws Exception {
    // initialize test properties and create the directories

    if (!QUICKTEST) {
      // initialize test properties and create the directories
      initialize(APP_PROPS_FILE);

      if (operator1 == null) {
        operator1 = TestUtils.createOperator(OPERATOR1_YAML);
      }
      TESTSCRIPTDIR = BaseTest.getProjectRoot() + "/integration-tests/src/test/resources/";
      domain = createSitConfigDomain();
      Assert.assertNotNull(domain);
      ADMINPODNAME = domain.getDomainUid() + "-" + domain.getAdminServerName();
      TestUtils.copyFileViaCat(
          TESTSCRIPTDIR + "sitconfig/java/SitConfigTests.java",
          "SitConfigTests.java",
          ADMINPODNAME,
          domain.getDomainNS());
      TestUtils.copyFileViaCat(
          TESTSCRIPTDIR + "sitconfig/scripts/runSitConfigTests.sh",
          "runSitConfigTests.sh",
          ADMINPODNAME,
          domain.getDomainNS());
      fqdn = TestUtils.getHostName();
    }
  }

  /**
   * Releases k8s cluster lease, archives result, pv directories
   *
   * @throws Exception
   */
  @AfterClass
  public static void staticUnPrepare() throws Exception {
    if (!QUICKTEST) {
      logger.info("+++++++++++++++++++++++++++++++++---------------------------------+");
      logger.info("BEGIN");
      logger.info("Run once, release cluster lease");

      destroySitConfigDomain();
      tearDown();
      logger.info("SUCCESS");
    }
  }

  /**
   * This test covers custom situational configuration use cases for config.xml. It sets the
   * connect-timeout, max-message-size, restart-max, JMXCore and Serverlifecycle debug flags. Also
   * sets the T3Channel public address using Kubernetes secret and verifies all these parameters are
   * overridden for domain
   *
   * @throws Exception
   */
  @Test
  public void testCustomSitConfigOverridesForDomain() throws Exception {
    Assume.assumeFalse(QUICKTEST);
    boolean testCompletedSuccessfully = false;
    String testMethod = new Object() {}.getClass().getEnclosingMethod().getName();
    logTestBegin(testMethod);
    String stdout =
        callShellScriptByExecToPod(
            "runSitConfigTests.sh",
            fqdn + " " + T3CHANNELPORT + " weblogic welcome1 " + testMethod,
            ADMINPODNAME,
            domain.getDomainNS());
    Assert.assertFalse(stdout.toLowerCase().contains("error"));
    testCompletedSuccessfully = true;
    logger.log(Level.INFO, "SUCCESS - {0}", testMethod);
  }

  /**
   * This test covers custom situational configuration use cases for JDBC resource. The resource
   * override sets the following connection pool properties. initialCapacity, maxCapacity,
   * test-connections-on-reserve, connection-harvest-max-count, inactive-connection-timeout-seconds
   * It also overrides the jdbc driver parameters like data source url, db user and password using
   * kubernetes secret.
   *
   * @throws Exception
   */
  @Test
  public void testCustomSitConfigOverridesForJdbc() throws Exception {
    Assume.assumeFalse(QUICKTEST);
    boolean testCompletedSuccessfully = false;
    String testMethod = new Object() {}.getClass().getEnclosingMethod().getName();
    logTestBegin(testMethod);
    String stdout =
        callShellScriptByExecToPod(
            "runSitConfigTests.sh",
            fqdn + " " + T3CHANNELPORT + " weblogic welcome1 " + testMethod,
            ADMINPODNAME,
            domain.getDomainNS());
    Assert.assertFalse(stdout.toLowerCase().contains("error"));
    testCompletedSuccessfully = true;
    logger.log(Level.INFO, "SUCCESS - {0}", testMethod);
  }

  /**
   * This test covers custom situational configuration use cases for JMS resource The resource
   * override file sets the following Delivery Failure Parameters. Redelivery limit and Expiration
   * policy
   *
   * @throws Exception
   */
  @Test
  public void testCustomSitConfigOverridesForJms() throws Exception {
    Assume.assumeFalse(QUICKTEST);
    boolean testCompletedSuccessfully = false;
    String testMethod = new Object() {}.getClass().getEnclosingMethod().getName();
    logTestBegin(testMethod);
    String stdout =
        callShellScriptByExecToPod(
            "runSitConfigTests.sh",
            fqdn + " " + T3CHANNELPORT + " weblogic welcome1 " + testMethod,
            ADMINPODNAME,
            domain.getDomainNS());
    Assert.assertFalse(stdout.toLowerCase().contains("error"));
    testCompletedSuccessfully = true;
    logger.log(Level.INFO, "SUCCESS - {0}", testMethod);
  }

  /**
   * This test covers custom situational configuration use cases for diagnostics resource It adds a
   * bunch of instrumentation monitors and harvesters in a diagnostics module.
   *
   * @throws Exception
   */
  @Test
  public void testCustomSitConfigOverridesForWldf() throws Exception {
    Assume.assumeFalse(QUICKTEST);
    boolean testCompletedSuccessfully = false;
    String testMethod = new Object() {}.getClass().getEnclosingMethod().getName();
    logTestBegin(testMethod);
    String stdout =
        callShellScriptByExecToPod(
            "runSitConfigTests.sh",
            fqdn + " " + T3CHANNELPORT + " weblogic welcome1 " + testMethod,
            ADMINPODNAME,
            domain.getDomainNS());
    Assert.assertFalse(stdout.toLowerCase().contains("error"));
    testCompletedSuccessfully = true;
    logger.log(Level.INFO, "SUCCESS - {0}", testMethod);
  }

  private static Domain createSitConfigDomain() throws Exception {
    String createDomainScript = TESTSCRIPTDIR + "/domain-home-on-pv/create-domain.py";
    // load input yaml to map and add configOverrides
    Map<String, Object> domainMap = TestUtils.loadYaml(DOMAINONPV_WLST_YAML);
    domainMap.put("configOverrides", "sitconfigcm");
    domainMap.put(
        "configOverridesFile", "/integration-tests/src/test/resources/sitconfig/configoverrides");
    domainMap.put("domainUID", DOMAINUID);
    domainMap.put("adminNodePort", new Integer(ADMINPORT));
    domainMap.put("t3ChannelPort", new Integer(T3CHANNELPORT));
    domainMap.put(
        "createDomainPyScript",
        "integration-tests/src/test/resources/sitconfig/scripts/create-domain-auto-custom-sit-config20.py");
    domainMap.put(
        "javaOptions",
        "-Dweblogic.debug.DebugSituationalConfig=true -Dweblogic.debug.DebugSituationalConfigDumpXml=true");

    domain = TestUtils.createDomain(domainMap);
    domain.verifyDomainCreated();
    return domain;
  }

  private static void destroySitConfigDomain() throws Exception {
    if (domain != null) {
      domain.destroy();
    }
  }

  private static String callShellScriptByExecToPod(
      String scriptPath, String arguments, String podName, String namespace) throws Exception {

    StringBuffer cmdKubectlSh = new StringBuffer("kubectl -n ");
    cmdKubectlSh
        .append(namespace)
        .append(" exec -it ")
        .append(podName)
        .append(" -- bash -c 'sh ")
        .append(scriptPath)
        .append(" ")
        .append(arguments)
        .append("'");
    logger.info("Command to call kubectl sh file " + cmdKubectlSh);
    ExecResult result = ExecCommand.exec(cmdKubectlSh.toString());
    if (result.exitValue() != 0) {
      logger.log(Level.INFO, result.stdout().trim());
      throw new RuntimeException(
          "FAILURE: command " + cmdKubectlSh + " failed, returned " + result.stderr());
    }
    logger.log(Level.INFO, result.stdout().trim());
    return result.stdout().trim();
  }
}