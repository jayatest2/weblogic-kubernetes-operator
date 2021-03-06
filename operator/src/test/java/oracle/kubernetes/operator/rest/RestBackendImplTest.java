// Copyright 2018, 2019, Oracle Corporation and/or its affiliates.  All rights reserved.
// Licensed under the Universal Permissive License v 1.0 as shown at
// http://oss.oracle.com/licenses/upl.

package oracle.kubernetes.operator.rest;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.StaticStubSupport;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1SubjectAccessReview;
import io.kubernetes.client.models.V1SubjectAccessReviewStatus;
import io.kubernetes.client.models.V1TokenReview;
import io.kubernetes.client.models.V1TokenReviewStatus;
import io.kubernetes.client.models.V1UserInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import oracle.kubernetes.TestUtils;
import oracle.kubernetes.operator.helpers.BodyMatcher;
import oracle.kubernetes.operator.helpers.CallTestSupport;
import oracle.kubernetes.operator.rest.RestBackendImpl.TopologyRetriever;
import oracle.kubernetes.operator.rest.backend.RestBackend;
import oracle.kubernetes.operator.utils.WlsDomainConfigSupport;
import oracle.kubernetes.operator.wlsconfig.WlsDomainConfig;
import oracle.kubernetes.weblogic.domain.ClusterConfigurator;
import oracle.kubernetes.weblogic.domain.DomainConfigurator;
import oracle.kubernetes.weblogic.domain.DomainConfiguratorFactory;
import oracle.kubernetes.weblogic.domain.v2.Domain;
import oracle.kubernetes.weblogic.domain.v2.DomainList;
import oracle.kubernetes.weblogic.domain.v2.DomainSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("SameParameterValue")
public class RestBackendImplTest {

  private static final int REPLICA_LIMIT = 4;
  private static final String DOMAIN = "domain";
  private static final String NS = "namespace1";
  private static final String UID = "uid1";
  private static final String UID2 = "uid2";
  private static List<Domain> domains = new ArrayList<>();
  private WlsDomainConfigSupport configSupport = new WlsDomainConfigSupport(DOMAIN);

  private List<Memento> mementos = new ArrayList<>();
  private RestBackend restBackend;
  private Domain domain = createDomain(NS, UID);
  private Domain domain2 = createDomain(NS, UID2);
  private DomainConfigurator configurator = DomainConfiguratorFactory.forDomain(domain);
  private CallTestSupport testSupport = new CallTestSupport();
  private Domain updatedDomain;
  private SecurityControl securityControl = new SecurityControl();
  private BodyMatcher fetchDomain =
      actualBody -> {
        updatedDomain = (Domain) actualBody;
        return true;
      };

  private static Domain createDomain(String namespace, String uid) {
    return new Domain()
        .withMetadata(new V1ObjectMeta().namespace(namespace))
        .withSpec(new DomainSpec().withDomainUID(uid));
  }

  private WlsDomainConfig config;

  private class TopologyRetrieverStub implements TopologyRetriever {
    @Override
    public WlsDomainConfig getWlsDomainConfig(String ns, String domainUID) {
      return config;
    }
  }

  @Before
  public void setUp() throws Exception {
    mementos.add(TestUtils.silenceOperatorLogger());
    mementos.add(testSupport.installSynchronousCallDispatcher());
    mementos.add(
        StaticStubSupport.install(RestBackendImpl.class, "INSTANCE", new TopologyRetrieverStub()));

    expectSecurityCalls();
    expectPossibleListDomainCall();
    expectPossibleReplaceDomainCall();

    domains.clear();
    domains.add(domain);
    domains.add(domain2);
    configSupport.addWlsCluster("cluster1", "ms1", "ms2", "ms3", "ms4", "ms5", "ms6");
    restBackend = new RestBackendImpl("", "", Collections.singletonList(NS));

    setupScanCache();
  }

  private void expectSecurityCalls() {
    testSupport
        .createCannedResponse("createTokenReview")
        .ignoringBody()
        .returning(securityControl.getTokenReviewResponse());
    testSupport
        .createCannedResponse("createSubjectAccessReview")
        .ignoringBody()
        .returning(securityControl.getSubjectAccessResponse());
  }

  private void expectPossibleListDomainCall() {
    testSupport
        .createOptionalCannedResponse("listDomain")
        .withNamespace(NS)
        .returning(new DomainList().withItems(domains));
  }

  private void expectPossibleReplaceDomainCall() {
    testSupport
        .createOptionalCannedResponse("replaceDomain")
        .withNamespace(NS)
        .withUid(UID)
        .withBody(fetchDomain)
        .returning(new Domain());

    testSupport
        .createOptionalCannedResponse("replaceDomain")
        .withNamespace(NS)
        .withUid(UID2)
        .withBody(fetchDomain)
        .failingWithStatus(HTTP_CONFLICT);
  }

  @After
  public void tearDown() {
    for (Memento memento : mementos) memento.revert();
    testSupport.verifyAllDefinedResponsesInvoked();
  }

  @Test(expected = WebApplicationException.class)
  public void whenNegativeScaleSpecified_throwException() {
    restBackend.scaleCluster(UID, "cluster1", -1);
  }

  @Test
  public void whenPerClusterReplicaSettingMatchesScaleRequest_doNothing() {
    configureCluster("cluster1").withReplicas(5);

    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain(), nullValue());
  }

  private Domain getUpdatedDomain() {
    return updatedDomain;
  }

  private ClusterConfigurator configureCluster(String clusterName) {
    return configureDomain().configureCluster(clusterName);
  }

  @Test
  public void whenPerClusterReplicaSetting_scaleClusterUpdatesSetting() {
    configureCluster("cluster1").withReplicas(1);

    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain().getReplicaCount("cluster1"), equalTo(5));
  }

  @Test
  public void whenNoPerClusterReplicaSetting_scaleClusterCreatesOne() {
    restBackend.scaleCluster(UID, "cluster1", 5);

    assertThat(getUpdatedDomain().getReplicaCount("cluster1"), equalTo(5));
  }

  @Test
  public void whenNoPerClusterReplicaSettingAndDefaultMatchesRequest_doNothing() {
    configureDomain().withDefaultReplicaCount(REPLICA_LIMIT);

    restBackend.scaleCluster(UID, "cluster1", REPLICA_LIMIT);

    assertThat(getUpdatedDomain(), nullValue());
  }

  @Test(expected = WebApplicationException.class)
  public void whenReplaceDomainReturnsError_scaleClusterThrowsException() {
    DomainConfiguratorFactory.forDomain(domain2).configureCluster("cluster1").withReplicas(2);

    restBackend.scaleCluster(UID2, "cluster1", 3);
  }

  @Test
  public void getDomainForConflictRetry_returnsBeforeDomain() {
    configureDomain().withDefaultReplicaCount(2);

    assertThat(
        ((RestBackendImpl) restBackend)
            .getDomainForConflictRetry(UID, "cluster1", REPLICA_LIMIT)
            .getReplicaCount("cluster-1"),
        equalTo(2));
  }

  @Test
  public void getDomainForConflictRetry_ifSameReplicaCount_returnsNull() {
    final int REPLICA_COUNT = REPLICA_LIMIT;
    configureDomain().withDefaultReplicaCount(REPLICA_COUNT);

    assertThat(
        ((RestBackendImpl) restBackend).getDomainForConflictRetry(UID, "cluster1", REPLICA_LIMIT),
        nullValue());
  }

  @Test(expected = WebApplicationException.class)
  public void getDomainForConflictRetry_ifDomainNotFound_throws() {
    configureDomain().withDefaultReplicaCount(2);

    ((RestBackendImpl) restBackend)
        .getDomainForConflictRetry("noSuchUID", "cluster1", REPLICA_LIMIT);
  }

  @Test
  public void verify_getWlsDomainConfig_returnsWlsDomainConfig() {
    WlsDomainConfig wlsDomainConfig = ((RestBackendImpl) restBackend).getWlsDomainConfig(UID);

    assertThat(wlsDomainConfig.getName(), equalTo(DOMAIN));
  }

  @Test
  public void verify_getWlsDomainConfig_doesNotReturnNull_whenNoSuchDomainUID() {
    WlsDomainConfig wlsDomainConfig =
        ((RestBackendImpl) restBackend).getWlsDomainConfig("NoSuchDomainUID");

    assertThat(wlsDomainConfig, notNullValue());
  }

  @Test
  public void verify_getWlsDomainConfig_doesNotReturnNull_whenScanIsNull() {
    config = null;

    WlsDomainConfig wlsDomainConfig = ((RestBackendImpl) restBackend).getWlsDomainConfig(UID);

    assertThat(wlsDomainConfig, notNullValue());
  }

  private DomainConfigurator configureDomain() {
    return configurator;
  }

  private static class SecurityControl {
    private final boolean allowed = true;
    private final boolean authenticated = true;

    private V1TokenReview getTokenReviewResponse() {
      return new V1TokenReview().status(getTokenReviewStatus());
    }

    private V1TokenReviewStatus getTokenReviewStatus() {
      return new V1TokenReviewStatus().authenticated(authenticated).user(new V1UserInfo());
    }

    private V1SubjectAccessReview getSubjectAccessResponse() {
      return new V1SubjectAccessReview().status(new V1SubjectAccessReviewStatus().allowed(allowed));
    }
  }

  void setupScanCache() {
    config = configSupport.createDomainConfig();
  }
}
